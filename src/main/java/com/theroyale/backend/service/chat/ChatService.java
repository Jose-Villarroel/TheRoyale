package com.theroyale.backend.service.chat;

import com.theroyale.backend.config.GeminiProperties;
import com.theroyale.backend.dto.ChatRequest;
import com.theroyale.backend.dto.TurnoChatRequest;
import com.theroyale.backend.errors.ReglaNegocioException;
import com.theroyale.backend.errors.ServicioNoDisponibleException;
import com.theroyale.backend.service.chat.GeminiClient.LlamadaFuncion;
import com.theroyale.backend.service.chat.GeminiClient.MensajeGemini;
import com.theroyale.backend.service.chat.GeminiClient.ParteGemini;
import com.theroyale.backend.service.chat.GeminiClient.SolicitudGemini;
import com.theroyale.backend.service.chat.HerramientasChatService.ResultadoHerramienta;
import org.springframework.context.MessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// ===== Conversación con el Royale Concierge (Gemini + herramientas + streaming SSE) =====
// Flujo de un mensaje:
// 1. Antes de abrir el stream se valida todo y los errores salen como JSON normal:
//    sin clave -> 503, mensaje vacío / > 1000 caracteres / rol inválido -> 400, límite por IP -> 429.
// 2. En un hilo aparte se llama a Gemini en streaming; cada texto se reenvía como evento "texto".
// 3. Si el modelo pide una herramienta, se ejecuta, se emiten sus tarjetas ("tarjetas") y se vuelve a
//    llamar a Gemini con la functionCall y la functionResponse (máximo 2 rondas de herramientas;
//    la siguiente llamada ya no permite herramientas y obliga a responder con texto).
// 4. Al final se emiten "sugerencias" y "fin". Si Gemini falla a mitad, se emite "error" (y no "fin").
// El backend no guarda la conversación: el historial llega del front y se recorta a los últimos turnos.
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    static final int LARGO_MAXIMO_MENSAJE = 1000;
    // Turnos previos muy largos (respuestas del asistente) se recortan para no gastar tokens
    static final int LARGO_MAXIMO_TURNO = 4000;
    static final int MAXIMO_RONDAS_HERRAMIENTAS = 2;

    static final String ROL_USUARIO = "usuario";
    static final String ROL_ASISTENTE = "asistente";

    // Codigos del contrato (docs/i18n-codigos-error.md); el texto sale de messages[_es].properties
    static final String CODIGO_OFFLINE = "chat.no-disponible";
    static final String CODIGO_ERROR = "chat.respuesta-incompleta";
    // No es un error (no viaja en "codigo"): es texto del stream, por eso tiene su propia clave
    static final String CLAVE_RESPUESTA_VACIA = "chat.respuesta-vacia";

    // ===== Nombres de los eventos SSE (contrato con chat.service.ts del front) =====
    public static final String EVENTO_TEXTO = "texto";
    public static final String EVENTO_TARJETAS = "tarjetas";
    public static final String EVENTO_SUGERENCIAS = "sugerencias";
    public static final String EVENTO_FIN = "fin";
    public static final String EVENTO_ERROR = "error";

    // Destino de los eventos: el SseEmitter en producción, una lista en las pruebas
    @FunctionalInterface
    public interface EmisorChat {
        void emitir(String evento, Object datos);
    }

    // El cliente cerró la pestaña o el stream ya terminó: se deja de trabajar sin emitir "error"
    static class ConexionCerradaException extends RuntimeException {
        ConexionCerradaException(Throwable causa) {
            super(causa);
        }
    }

    private final GeminiClient geminiClient;
    private final GeminiProperties propiedades;
    private final ContextoHotelService contextoHotelService;
    private final HerramientasChatService herramientasChatService;
    private final LimiteMensajesService limiteMensajesService;
    private final TaskExecutor ejecutor;
    private final MessageSource messageSource;

    public ChatService(GeminiClient geminiClient, GeminiProperties propiedades,
                       ContextoHotelService contextoHotelService, HerramientasChatService herramientasChatService,
                       LimiteMensajesService limiteMensajesService,
                       @Qualifier("applicationTaskExecutor") TaskExecutor ejecutor,
                       MessageSource messageSource) {
        this.geminiClient = geminiClient;
        this.propiedades = propiedades;
        this.contextoHotelService = contextoHotelService;
        this.herramientasChatService = herramientasChatService;
        this.limiteMensajesService = limiteMensajesService;
        this.ejecutor = ejecutor;
        this.messageSource = messageSource;
    }

    public boolean estaDisponible() {
        return propiedades.tieneClave();
    }

    // ===== Punto de entrada de POST /api/chat =====
    // Lo que lanza aquí (antes de devolver el emisor) llega al cliente como JSON {status, message}.
    // El idioma llega resuelto (AcceptHeaderLocaleResolver, ver I18nConfig) y se captura aqui: el
    // stream corre en otro hilo y LocaleContextHolder no viaja con el, asi que hay que pasarlo.
    public SseEmitter iniciarConversacion(ChatRequest solicitud, String ip, Locale idioma) {
        if (!estaDisponible()) {
            throw ServicioNoDisponibleException.conCodigo(CODIGO_OFFLINE);
        }
        ChatRequest preparada = prepararSolicitud(solicitud);
        limiteMensajesService.registrar(ip);

        // Margen para todas las rondas: cada llamada a Gemini tiene su propio timeout de lectura y, si
        // el modelo principal falla, puede repetirse con cada modelo de respaldo
        long llamadasPorRonda = propiedades.modelosEnOrden().size();
        long timeoutMs = (propiedades.getTimeoutSegundos() * llamadasPorRonda * (MAXIMO_RONDAS_HERRAMIENTAS + 1L) + 30) * 1000;
        SseEmitter emisor = new SseEmitter(timeoutMs);
        ejecutor.execute(() -> {
            try {
                responder(preparada, (evento, datos) -> enviar(emisor, evento, datos), idioma);
                emisor.complete();
            } catch (ConexionCerradaException ex) {
                log.debug("The client closed the chat before the answer was finished");
            }
        });
        return emisor;
    }

    // ===== Validación y recorte del historial =====
    // El historial se RECORTA a los últimos gemini.max-turnos (no se rechaza): así el front puede
    // mandar toda la conversación sin preocuparse. Turnos con texto vacío se descartan.
    public ChatRequest prepararSolicitud(ChatRequest solicitud) {
        if (solicitud == null || solicitud.mensaje() == null || solicitud.mensaje().isBlank()) {
            throw ReglaNegocioException.badRequest("chat.mensaje-vacio");
        }
        String mensaje = solicitud.mensaje().trim();
        if (mensaje.length() > LARGO_MAXIMO_MENSAJE) {
            throw ReglaNegocioException.badRequest("chat.mensaje-longitud", LARGO_MAXIMO_MENSAJE);
        }

        List<TurnoChatRequest> turnos = new ArrayList<>();
        if (solicitud.historial() != null) {
            for (TurnoChatRequest turno : solicitud.historial()) {
                if (turno == null || turno.rol() == null
                        || !(ROL_USUARIO.equals(turno.rol()) || ROL_ASISTENTE.equals(turno.rol()))) {
                    throw ReglaNegocioException.badRequest("chat.rol-invalido");
                }
                if (turno.texto() == null || turno.texto().isBlank()) {
                    continue;
                }
                String texto = turno.texto().trim();
                if (texto.length() > LARGO_MAXIMO_TURNO) {
                    texto = texto.substring(0, LARGO_MAXIMO_TURNO);
                }
                turnos.add(new TurnoChatRequest(turno.rol(), texto));
            }
        }

        int maximo = Math.max(0, propiedades.getMaxTurnos());
        List<TurnoChatRequest> recientes = new ArrayList<>(
                turnos.subList(Math.max(0, turnos.size() - maximo), turnos.size()));
        // La conversación que recibe Gemini empieza siempre con un turno del cliente
        while (!recientes.isEmpty() && ROL_ASISTENTE.equals(recientes.get(0).rol())) {
            recientes.remove(0);
        }
        return new ChatRequest(mensaje, recientes);
    }

    // ===== Bucle de Gemini y herramientas; emite los eventos en orden =====
    // Recibe una solicitud ya preparada (prepararSolicitud). Es síncrono: en producción corre en el ejecutor.
    public void responder(ChatRequest solicitud, EmisorChat emisor, Locale idioma) {
        try {
            String instruccion = contextoHotelService.construirInstruccionSistema(idioma);
            List<Map<String, Object>> declaraciones = herramientasChatService.declaraciones();
            List<MensajeGemini> contenidos = convertirHistorial(solicitud);
            FiltroSugerencias filtro = new FiltroSugerencias();
            Set<String> herramientasUsadas = new LinkedHashSet<>();

            for (int ronda = 0; ; ronda++) {
                boolean herramientasHabilitadas = ronda < MAXIMO_RONDAS_HERRAMIENTAS;
                List<ParteGemini> partesModelo = new ArrayList<>();
                List<LlamadaFuncion> llamadas = new ArrayList<>();

                geminiClient.transmitir(
                        new SolicitudGemini(instruccion, List.copyOf(contenidos), declaraciones, herramientasHabilitadas),
                        parte -> {
                            partesModelo.add(parte);
                            if (parte.llamada() != null) {
                                llamadas.add(parte.llamada());
                            } else if (parte.texto() != null) {
                                emitirTexto(emisor, filtro.procesar(parte.texto()));
                            }
                        });

                if (llamadas.isEmpty() || !herramientasHabilitadas) {
                    break;
                }

                // Se devuelve a Gemini su propio turno (con la functionCall) y el resultado de cada herramienta
                contenidos.add(new MensajeGemini("model", partesModelo));
                List<ParteGemini> respuestas = new ArrayList<>();
                for (LlamadaFuncion llamada : llamadas) {
                    ResultadoHerramienta resultado = herramientasChatService.ejecutar(llamada);
                    herramientasUsadas.add(llamada.nombre());
                    if (resultado.tarjetas() != null) {
                        emisor.emitir(EVENTO_TARJETAS, resultado.tarjetas());
                    }
                    respuestas.add(ParteGemini.deRespuesta(llamada.nombre(), resultado.respuesta()));
                }
                contenidos.add(new MensajeGemini("user", respuestas));
            }

            emitirTexto(emisor, filtro.finalizar());
            if (!filtro.huboTextoVisible()) {
                emitirTexto(emisor, mensaje(CLAVE_RESPUESTA_VACIA, idioma, ContextoHotelService.TELEFONO_RECEPCION));
            }

            List<String> sugerencias = filtro.sugerencias();
            if (sugerencias.size() < 2) {
                sugerencias = SugerenciasPredeterminadas.para(herramientasUsadas, idioma);
            }
            emisor.emitir(EVENTO_SUGERENCIAS, Map.of("sugerencias", sugerencias));
            emisor.emitir(EVENTO_FIN, Map.of());
        } catch (ConexionCerradaException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            // El detalle (código HTTP de Gemini, etc.) solo va al log; al cliente un mensaje genérico
            log.warn("The chat answer failed: {}", ex.getMessage());
            // Aqui ya se respondio 200 y el stream esta abierto: el 502 del contrato no se puede
            // mandar, asi que el codigo y su mensaje viajan dentro del evento SSE "error".
            emisor.emitir(EVENTO_ERROR, Map.of("codigo", CODIGO_ERROR, "message", mensaje(CODIGO_ERROR, idioma)));
        }
    }

    private String mensaje(String clave, Locale idioma, Object... args) {
        return messageSource.getMessage(clave, args, clave, idioma == null ? Locale.ENGLISH : idioma);
    }

    private List<MensajeGemini> convertirHistorial(ChatRequest solicitud) {
        List<MensajeGemini> contenidos = new ArrayList<>();
        for (TurnoChatRequest turno : solicitud.historial()) {
            String rol = ROL_ASISTENTE.equals(turno.rol()) ? "model" : "user";
            contenidos.add(new MensajeGemini(rol, List.of(ParteGemini.deTexto(turno.texto()))));
        }
        contenidos.add(new MensajeGemini("user", List.of(ParteGemini.deTexto(solicitud.mensaje()))));
        return contenidos;
    }

    private void emitirTexto(EmisorChat emisor, String fragmento) {
        if (fragmento != null && !fragmento.isEmpty()) {
            emisor.emitir(EVENTO_TEXTO, Map.of("fragmento", fragmento));
        }
    }

    private void enviar(SseEmitter emisor, String evento, Object datos) {
        try {
            emisor.send(SseEmitter.event().name(evento).data(datos, MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException ex) {
            throw new ConexionCerradaException(ex);
        }
    }
}
