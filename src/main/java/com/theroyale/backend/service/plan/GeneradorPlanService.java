package com.theroyale.backend.service.plan;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theroyale.backend.config.HotelProperties;
import com.theroyale.backend.dto.plan.PreferenciasPlan;
import com.theroyale.backend.errors.PlanNoConfiableException;
import com.theroyale.backend.errors.ServicioNoDisponibleException;
import com.theroyale.backend.model.TipoBloque;
import com.theroyale.backend.service.chat.GeminiClient;
import com.theroyale.backend.service.chat.GeminiClient.MensajeGemini;
import com.theroyale.backend.service.chat.GeminiClient.ParteGemini;
import com.theroyale.backend.service.chat.GeminiClient.RespuestaJson;
import com.theroyale.backend.service.chat.GeminiClient.SolicitudGemini;
import com.theroyale.backend.service.chat.GeminiException;
import com.theroyale.backend.service.plan.ContextoPlan.EventoContexto;
import com.theroyale.backend.service.plan.ContextoPlan.LugarContexto;
import com.theroyale.backend.service.plan.fuentes.ClimaDia;
import com.theroyale.backend.service.plan.fuentes.Distancias;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// ===== Pide a Gemini el plan en JSON con el esquema de la sección 7 =====
// La instrucción de sistema está en resources/plan/instruccion-plan.md. El mensaje lleva solo fechas,
// número de huéspedes, tipo de habitación, preferencias, clima por día y las cuatro listas con sus IDs:
// nunca nombre, email, cédula ni teléfono del huésped. Si Gemini devuelve un JSON ilegible se reintenta
// una vez; si Gemini no responde (con todos sus modelos de respaldo) se responde 503.
@Service
public class GeneradorPlanService {

    static final String MENSAJE_NO_DISPONIBLE = "The planner is unavailable right now.";
    private static final String RUTA_INSTRUCCION = "plan/instruccion-plan.md";
    private static final Logger log = LoggerFactory.getLogger(GeneradorPlanService.class);

    @Autowired
    private GeminiClient geminiClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HotelProperties hotel;

    private String instruccion;

    // ===== Datos de una solicitud de generación =====
    // planActual y otrosDias son resúmenes del plan guardado ("10:00 LUGAR The Met"), solo para contexto.
    public record Solicitud(DatosEstadia estadia, PreferenciasPlan preferencias, String descripcion,
                            String indicacion, List<LocalDate> diasAPlanificar,
                            Map<String, List<String>> planActual, Map<String, List<String>> otrosDias) {
    }

    public PlanGenerado.Respuesta generar(ContextoPlan contexto, Solicitud solicitud) {
        SolicitudGemini peticion = new SolicitudGemini(instruccion(),
                List.of(new MensajeGemini("user", List.of(ParteGemini.deTexto(construirMensaje(contexto, solicitud))))),
                List.of(), false);

        for (int intento = 1; intento <= 2; intento++) {
            RespuestaJson respuesta;
            try {
                respuesta = geminiClient.generarJson(peticion, esquema());
            } catch (GeminiException ex) {
                log.warn("Gemini no pudo generar el plan: {}", ex.getMessage());
                throw new ServicioNoDisponibleException(MENSAJE_NO_DISPONIBLE);
            }
            try {
                PlanGenerado plan = objectMapper.readValue(respuesta.json(), PlanGenerado.class);
                if (plan.dias() != null) {
                    return new PlanGenerado.Respuesta(plan, respuesta.modelo());
                }
                log.warn("Gemini devolvió un plan sin días (intento {})", intento);
            } catch (JsonProcessingException ex) {
                log.warn("Gemini devolvió un plan ilegible (intento {}): {}", intento, ex.getOriginalMessage());
            }
        }
        throw new PlanNoConfiableException();
    }

    // ===== Mensaje con el contexto en JSON =====
    String construirMensaje(ContextoPlan contexto, Solicitud solicitud) {
        DatosEstadia estadia = solicitud.estadia();
        PreferenciasPlan preferencias = solicitud.preferencias();
        Map<String, Object> mensaje = new LinkedHashMap<>();

        // Los modelos rápidos tienden a responder en inglés aunque el huésped escriba en otro idioma:
        // se les da una muestra explícita del texto del huésped al inicio del mensaje.
        String muestra = muestraDeIdioma(solicitud);
        mensaje.put("idiomaDeRespuesta", muestra == null
                ? "English"
                : "Write resumen, every day titulo and every motivo in the SAME language as this guest text: \"" + muestra + "\"");

        Map<String, Object> datosEstadia = new LinkedHashMap<>();
        datosEstadia.put("llegada", estadia.fechaInicio().toString());
        datosEstadia.put("salida", estadia.fechaFin().toString());
        datosEstadia.put("noches", estadia.noches());
        datosEstadia.put("huespedes", estadia.cantidadPersonas());
        datosEstadia.put("habitacion", estadia.tipoHabitacion());
        datosEstadia.put("checkIn", DatosEstadia.CHECK_IN.toString());
        datosEstadia.put("checkOut", DatosEstadia.CHECK_OUT.toString());
        datosEstadia.put("hotel", hotel.getDireccion());
        mensaje.put("estadia", datosEstadia);
        mensaje.put("preferencias", preferencias);
        if (solicitud.descripcion() != null && !solicitud.descripcion().isBlank()) {
            mensaje.put("descripcionDelHuesped", solicitud.descripcion());
        }
        if (solicitud.indicacion() != null && !solicitud.indicacion().isBlank()) {
            mensaje.put("indicacion", solicitud.indicacion());
        }
        if (solicitud.planActual() != null && !solicitud.planActual().isEmpty()) {
            mensaje.put("planActual", solicitud.planActual());
        }
        if (solicitud.otrosDias() != null && !solicitud.otrosDias().isEmpty()) {
            mensaje.put("otrosDias", solicitud.otrosDias());
        }

        List<Map<String, Object>> dias = new ArrayList<>();
        for (LocalDate fecha : solicitud.diasAPlanificar()) {
            Map<String, Object> dia = new LinkedHashMap<>();
            String tipoDia = estadia.tipoDia(fecha);
            dia.put("fecha", fecha.toString());
            dia.put("diaDeLaSemana", fecha.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH));
            dia.put("tipoDia", tipoDia);
            if ("LLEGADA".equals(tipoDia)) {
                dia.put("limite", "blocks start at 15:30 or later");
            } else if ("SALIDA".equals(tipoDia)) {
                dia.put("limite", "blocks end at 12:00 or earlier");
            }
            ClimaDia clima = contexto.clima().get(fecha);
            if (clima != null) {
                dia.put("clima", Map.of("resumen", clima.resumen(), "maxC", clima.maxC(), "minC", clima.minC(),
                        "probLluvia", clima.probLluviaOCero(), "tipo", clima.tipo().name()));
            }
            dias.add(dia);
        }
        mensaje.put("diasAPlanificar", dias);

        mensaje.put("SERVICIOS", contexto.servicios().stream().map(s -> {
            Map<String, Object> servicio = new LinkedHashMap<>();
            servicio.put("servicioId", String.valueOf(s.id()));
            servicio.put("nombre", s.nombre());
            servicio.put("precioUsd", s.precio());
            servicio.put("caracteristicas", s.caracteristicas());
            return servicio;
        }).toList());
        mensaje.put("RESTAURANTES", contexto.restaurantes().stream().map(r -> {
            Map<String, Object> restaurante = new LinkedHashMap<>();
            restaurante.put("referenciaId", r.restaurante().name());
            restaurante.put("nombre", r.nombre());
            restaurante.put("horario", r.horario());
            if (!r.opcionesDieta().isEmpty()) {
                restaurante.put("opcionesDieta", r.opcionesDieta());
            }
            return restaurante;
        }).toList());
        mensaje.put("LUGARES", contexto.lugares().stream().map(this::lugarParaGemini).toList());
        mensaje.put("EVENTOS", contexto.eventos().stream().map(this::eventoParaGemini).toList());

        try {
            return objectMapper.writeValueAsString(mensaje);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo armar el mensaje del plan", ex);
        }
    }

    // Primeros 120 caracteres de lo que escribió el huésped (descripción, notas o indicación)
    private static String muestraDeIdioma(Solicitud solicitud) {
        for (String texto : new String[]{solicitud.descripcion(),
                solicitud.preferencias() == null ? null : solicitud.preferencias().notas(), solicitud.indicacion()}) {
            if (texto != null && !texto.isBlank()) {
                String limpio = texto.trim().replace('"', '\'');
                return limpio.length() > 120 ? limpio.substring(0, 120) : limpio;
            }
        }
        return null;
    }

    private Map<String, Object> lugarParaGemini(LugarContexto lugar) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("lugarId", String.valueOf(lugar.id()));
        datos.put("nombre", lugar.nombre());
        datos.put("categoria", lugar.categoria().name());
        datos.put("bajoTecho", lugar.bajoTecho());
        datos.put("duracionMinutos", lugar.duracionMinutos());
        datos.put("horario", lugar.horario());
        datos.put("minutosAPie", Distancias.minutosAPie(lugar.distanciaMetros()));
        return datos;
    }

    private Map<String, Object> eventoParaGemini(EventoContexto contexto) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("eventoId", contexto.evento().id());
        datos.put("titulo", contexto.evento().titulo());
        datos.put("inicio", contexto.evento().inicio().toString());
        datos.put("fin", contexto.evento().fin().toString());
        datos.put("lugar", contexto.evento().lugar());
        datos.put("categoria", contexto.evento().categoria());
        datos.put("bajoTecho", contexto.evento().bajoTecho());
        if (contexto.distanciaMetros() != null) {
            datos.put("minutosAPie", Distancias.minutosAPie(contexto.distanciaMetros()));
        }
        return datos;
    }

    // ===== Esquema de respuesta (formato responseSchema de Gemini) =====
    static Map<String, Object> esquema() {
        Map<String, Object> bloque = objeto(Map.of(
                "inicio", texto(), "fin", texto(),
                "tipo", Map.of("type", "STRING", "enum", Arrays.stream(TipoBloque.values()).map(Enum::name).toList()),
                "referenciaId", texto(), "caracteristica", texto(), "titulo", texto(), "motivo", texto()),
                List.of("inicio", "fin", "tipo", "referenciaId", "caracteristica", "titulo", "motivo"),
                List.of("inicio", "fin", "tipo", "referenciaId", "titulo", "motivo"));
        Map<String, Object> dia = objeto(Map.of(
                "fecha", texto(), "titulo", texto(), "bloques", Map.of("type", "ARRAY", "items", bloque)),
                List.of("fecha", "titulo", "bloques"), List.of("fecha", "titulo", "bloques"));
        return objeto(Map.of("resumen", texto(), "dias", Map.of("type", "ARRAY", "items", dia)),
                List.of("resumen", "dias"), List.of("resumen", "dias"));
    }

    static Map<String, Object> texto() {
        return Map.of("type", "STRING");
    }

    static Map<String, Object> objeto(Map<String, Object> propiedades, List<String> orden, List<String> requeridas) {
        Map<String, Object> esquema = new LinkedHashMap<>();
        esquema.put("type", "OBJECT");
        esquema.put("properties", propiedades);
        esquema.put("propertyOrdering", orden);
        esquema.put("required", requeridas);
        return esquema;
    }

    private String instruccion() {
        if (instruccion == null) {
            try {
                instruccion = new ClassPathResource(RUTA_INSTRUCCION).getContentAsString(StandardCharsets.UTF_8);
            } catch (IOException ex) {
                throw new UncheckedIOException("No se encontró " + RUTA_INSTRUCCION, ex);
            }
        }
        return instruccion;
    }
}
