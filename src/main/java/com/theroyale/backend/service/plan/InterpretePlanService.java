package com.theroyale.backend.service.plan;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theroyale.backend.dto.plan.EntendidoItem;
import com.theroyale.backend.dto.plan.InterpretacionPlan;
import com.theroyale.backend.dto.plan.PreferenciasPlan;
import com.theroyale.backend.errors.PlanNoConfiableException;
import com.theroyale.backend.errors.ServicioNoDisponibleException;
import com.theroyale.backend.model.EtiquetaPlato;
import com.theroyale.backend.model.Interes;
import com.theroyale.backend.model.Presupuesto;
import com.theroyale.backend.model.Ritmo;
import com.theroyale.backend.service.chat.GeminiClient;
import com.theroyale.backend.service.chat.GeminiClient.MensajeGemini;
import com.theroyale.backend.service.chat.GeminiClient.ParteGemini;
import com.theroyale.backend.service.chat.GeminiClient.SolicitudGemini;
import com.theroyale.backend.service.chat.GeminiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// ===== Convierte la descripción libre del viaje en PreferenciasPlan y 3 a 5 frases de "What I understood" =====
// Una llamada corta a Gemini con generarJson (instrucción en resources/plan/instruccion-interprete.md).
// Solo se le mandan el texto del huésped y las fechas, noches y número de huéspedes de la reserva.
// Lo que devuelve se normaliza: valores fuera de los enums se descartan, se recorta a los límites del
// contrato y, si faltan, se usan valores por defecto razonables.
@Service
public class InterpretePlanService {

    static final int MINIMO_ENTENDIDO = 3;
    static final int MAXIMO_ENTENDIDO = 5;
    static final int MAXIMO_INTERESES = 5;
    static final int MAXIMO_NOTAS = 300;
    private static final String RUTA_INSTRUCCION = "plan/instruccion-interprete.md";
    private static final List<EtiquetaPlato> DIETAS =
            List.of(EtiquetaPlato.VEGANO, EtiquetaPlato.VEGETARIANO, EtiquetaPlato.SIN_GLUTEN);
    private static final Logger log = LoggerFactory.getLogger(InterpretePlanService.class);

    @Autowired
    private GeminiClient geminiClient;

    @Autowired
    private ObjectMapper objectMapper;

    private String instruccion;

    // Lo que devuelve Gemini, con todo como texto para tolerar valores inesperados
    @JsonIgnoreProperties(ignoreUnknown = true)
    record PreferenciasCrudas(List<String> intereses, String ritmo, String presupuesto, List<String> dieta,
                              Boolean movilidadReducida, Boolean conNinos, String notas) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record InterpretacionCruda(PreferenciasCrudas preferencias, List<EntendidoItem> entendido) {
    }

    public InterpretacionPlan interpretar(String descripcion, DatosEstadia estadia) {
        Map<String, Object> mensaje = new LinkedHashMap<>();
        mensaje.put("descripcion", descripcion);
        mensaje.put("estadia", Map.of("llegada", estadia.fechaInicio().toString(), "salida", estadia.fechaFin().toString(),
                "noches", estadia.noches(), "huespedes", estadia.cantidadPersonas()));

        SolicitudGemini solicitud;
        try {
            solicitud = new SolicitudGemini(instruccion(),
                    List.of(new MensajeGemini("user", List.of(ParteGemini.deTexto(objectMapper.writeValueAsString(mensaje))))),
                    List.of(), false);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo armar el mensaje de interpretación", ex);
        }

        String json;
        try {
            json = geminiClient.generarJson(solicitud, esquema()).json();
        } catch (GeminiException ex) {
            log.warn("Gemini no pudo interpretar la descripción: {}", ex.getMessage());
            throw new ServicioNoDisponibleException(GeneradorPlanService.MENSAJE_NO_DISPONIBLE);
        }

        try {
            return normalizar(objectMapper.readValue(json, InterpretacionCruda.class));
        } catch (JsonProcessingException ex) {
            log.warn("Gemini devolvió una interpretación ilegible: {}", ex.getOriginalMessage());
            throw new PlanNoConfiableException();
        }
    }

    InterpretacionPlan normalizar(InterpretacionCruda cruda) {
        PreferenciasCrudas p = cruda.preferencias() == null
                ? new PreferenciasCrudas(null, null, null, null, false, false, null) : cruda.preferencias();

        Set<Interes> intereses = new LinkedHashSet<>();
        for (String valor : p.intereses() == null ? List.<String>of() : p.intereses()) {
            enumOrNull(Interes.class, valor).ifPresent(intereses::add);
        }
        if (intereses.isEmpty()) {
            intereses.add(Interes.MUSEOS);
            intereses.add(Interes.PARQUES);
        }
        List<Interes> listaIntereses = intereses.stream().limit(MAXIMO_INTERESES).toList();

        Set<EtiquetaPlato> dieta = new LinkedHashSet<>();
        for (String valor : p.dieta() == null ? List.<String>of() : p.dieta()) {
            enumOrNull(EtiquetaPlato.class, valor).filter(DIETAS::contains).ifPresent(dieta::add);
        }

        String notas = p.notas() == null ? null : p.notas().trim();
        if (notas != null && notas.isEmpty()) {
            notas = null;
        }
        if (notas != null && notas.length() > MAXIMO_NOTAS) {
            notas = notas.substring(0, MAXIMO_NOTAS).trim();
        }

        PreferenciasPlan preferencias = new PreferenciasPlan(listaIntereses,
                enumOrNull(Ritmo.class, p.ritmo()).orElse(Ritmo.EQUILIBRADO),
                enumOrNull(Presupuesto.class, p.presupuesto()).orElse(Presupuesto.ALTO),
                List.copyOf(dieta),
                Boolean.TRUE.equals(p.movilidadReducida()),
                Boolean.TRUE.equals(p.conNinos()),
                notas);

        List<EntendidoItem> entendido = new ArrayList<>();
        for (EntendidoItem item : cruda.entendido() == null ? List.<EntendidoItem>of() : cruda.entendido()) {
            if (item != null && item.titulo() != null && !item.titulo().isBlank() && entendido.size() < MAXIMO_ENTENDIDO) {
                entendido.add(new EntendidoItem(item.titulo().trim(), item.detalle() == null ? "" : item.detalle().trim()));
            }
        }
        // Si la IA devolvió menos de 3, se completa con lo que dicen las preferencias
        List<EntendidoItem> respaldo = List.of(
                new EntendidoItem("Your interests.", "I will build each day around "
                        + String.join(", ", listaIntereses.stream().map(i -> i.name().toLowerCase(Locale.ROOT).replace('_', ' ')).toList()) + "."),
                new EntendidoItem("Your pace.", "I will plan about " + preferencias.ritmo().getBloquesPorDia()
                        + " activities a day, with time to rest."),
                new EntendidoItem("Close to the hotel.", "I will group each day by area, starting from 640 Fifth Avenue."));
        for (int i = 0; entendido.size() < MINIMO_ENTENDIDO && i < respaldo.size(); i++) {
            entendido.add(respaldo.get(i));
        }
        return new InterpretacionPlan(preferencias, List.copyOf(entendido));
    }

    static Map<String, Object> esquema() {
        Map<String, Object> preferencias = GeneradorPlanService.objeto(Map.of(
                        "intereses", Map.of("type", "ARRAY", "items", enumeracion(Interes.values())),
                        "ritmo", enumeracion(Ritmo.values()),
                        "presupuesto", enumeracion(Presupuesto.values()),
                        "dieta", Map.of("type", "ARRAY", "items", Map.of("type", "STRING",
                                "enum", DIETAS.stream().map(Enum::name).toList())),
                        "movilidadReducida", Map.of("type", "BOOLEAN"),
                        "conNinos", Map.of("type", "BOOLEAN"),
                        "notas", GeneradorPlanService.texto()),
                List.of("intereses", "ritmo", "presupuesto", "dieta", "movilidadReducida", "conNinos", "notas"),
                List.of("intereses", "ritmo", "presupuesto", "dieta", "movilidadReducida", "conNinos", "notas"));
        Map<String, Object> item = GeneradorPlanService.objeto(
                Map.of("titulo", GeneradorPlanService.texto(), "detalle", GeneradorPlanService.texto()),
                List.of("titulo", "detalle"), List.of("titulo", "detalle"));
        return GeneradorPlanService.objeto(Map.of("preferencias", preferencias,
                        "entendido", Map.of("type", "ARRAY", "items", item)),
                List.of("preferencias", "entendido"), List.of("preferencias", "entendido"));
    }

    private static Map<String, Object> enumeracion(Enum<?>[] valores) {
        return Map.of("type", "STRING", "enum", Arrays.stream(valores).map(Enum::name).toList());
    }

    private static <E extends Enum<E>> java.util.Optional<E> enumOrNull(Class<E> tipo, String valor) {
        if (valor == null || valor.isBlank()) {
            return java.util.Optional.empty();
        }
        try {
            return java.util.Optional.of(Enum.valueOf(tipo, valor.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            return java.util.Optional.empty();
        }
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
