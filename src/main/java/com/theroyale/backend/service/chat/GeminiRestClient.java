package com.theroyale.backend.service.chat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theroyale.backend.config.GeminiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

// ===== Implementación de GeminiClient sobre RestClient de Spring 6.1 (sin dependencias nuevas) =====
// Llama a models/{modelo}:streamGenerateContent?alt=sse y lee la respuesta línea por línea:
// cada línea "data: {...}" es un GenerateContentResponse con una o más partes.
// La clave viaja en el header x-goog-api-key (no en la URL, para que no quede en logs).
@Component
public class GeminiRestClient implements GeminiClient {

    private static final String URL_BASE = "https://generativelanguage.googleapis.com/v1beta";
    private static final int TIMEOUT_CONEXION_MS = 10_000;
    private static final double TEMPERATURA = 0.5;
    private static final Logger log = LoggerFactory.getLogger(GeminiRestClient.class);

    private final GeminiProperties propiedades;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    @Autowired
    public GeminiRestClient(GeminiProperties propiedades, ObjectMapper objectMapper) {
        this(propiedades, objectMapper, URL_BASE);
    }

    // Para las pruebas: apunta a un servidor local que imita a Gemini
    GeminiRestClient(GeminiProperties propiedades, ObjectMapper objectMapper, String urlBase) {
        this.propiedades = propiedades;
        this.objectMapper = objectMapper;

        // El read timeout se aplica a cada lectura del socket: corta si Gemini deja de enviar datos
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(TIMEOUT_CONEXION_MS);
        fabrica.setReadTimeout(propiedades.getTimeoutSegundos() * 1000);
        this.restClient = RestClient.builder()
                .baseUrl(urlBase)
                .requestFactory(fabrica)
                .build();
    }

    // Prueba los modelos en orden (principal y luego respaldos). Solo pasa al siguiente si el que falló
    // todavía no había entregado nada: si ya empezó a responder, reintentar duplicaría el texto.
    @Override
    public void transmitir(SolicitudGemini solicitud, Consumer<ParteGemini> alRecibirParte) {
        Map<String, Object> cuerpo = construirCuerpo(solicitud);
        GeminiException ultimoError = null;

        for (String modelo : propiedades.modelosEnOrden()) {
            boolean[] entregoAlgo = {false};
            try {
                llamarModelo(modelo, cuerpo, parte -> {
                    entregoAlgo[0] = true;
                    alRecibirParte.accept(parte);
                });
                return;
            } catch (GeminiException ex) {
                if (entregoAlgo[0]) {
                    throw ex;
                }
                log.warn("Gemini falló con el modelo {}; se prueba el siguiente si hay: {}", modelo, ex.getMessage());
                ultimoError = ex;
            }
        }
        throw ultimoError != null ? ultimoError : new GeminiException("No hay ningún modelo de Gemini configurado.");
    }

    private void llamarModelo(String modelo, Map<String, Object> cuerpo, Consumer<ParteGemini> alRecibirParte) {
        try {
            restClient.post()
                    .uri("/models/{modelo}:streamGenerateContent?alt=sse", modelo)
                    .header("x-goog-api-key", propiedades.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .body(cuerpo)
                    .exchange((peticion, respuesta) -> {
                        if (respuesta.getStatusCode().isError()) {
                            String detalle = new String(respuesta.getBody().readAllBytes(), StandardCharsets.UTF_8);
                            throw new GeminiException("Gemini respondió " + respuesta.getStatusCode().value()
                                    + ": " + recortar(detalle));
                        }
                        leerEventos(respuesta.getBody(), alRecibirParte);
                        return null;
                    });
        } catch (RestClientException ex) {
            throw new GeminiException("No se pudo completar la llamada a Gemini: " + ex.getMessage(), ex);
        }
    }

    private void leerEventos(InputStream cuerpo, Consumer<ParteGemini> alRecibirParte) throws IOException {
        try (BufferedReader lector = new BufferedReader(new InputStreamReader(cuerpo, StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                if (!linea.startsWith("data:")) {
                    continue;
                }
                String json = linea.substring(5).trim();
                if (!json.isEmpty()) {
                    procesarFragmento(objectMapper.readTree(json), alRecibirParte);
                }
            }
        }
    }

    private void procesarFragmento(JsonNode fragmento, Consumer<ParteGemini> alRecibirParte) {
        JsonNode bloqueo = fragmento.path("promptFeedback").path("blockReason");
        if (!bloqueo.isMissingNode()) {
            throw new GeminiException("Gemini bloqueó la consulta: " + bloqueo.asText());
        }

        JsonNode candidato = fragmento.path("candidates").path(0);
        for (JsonNode parte : candidato.path("content").path("parts")) {
            // Los resúmenes de razonamiento ("thought": true) no se muestran al cliente
            if (parte.path("thought").asBoolean(false)) {
                continue;
            }
            String firma = parte.hasNonNull("thoughtSignature") ? parte.get("thoughtSignature").asText() : null;

            if (parte.has("functionCall")) {
                JsonNode llamada = parte.get("functionCall");
                Map<String, Object> argumentos = llamada.hasNonNull("args")
                        ? objectMapper.convertValue(llamada.get("args"), new TypeReference<Map<String, Object>>() { })
                        : Map.of();
                alRecibirParte.accept(new ParteGemini(null,
                        new LlamadaFuncion(llamada.path("name").asText(), argumentos), null, firma));
            } else if (parte.has("text") || firma != null) {
                alRecibirParte.accept(new ParteGemini(parte.path("text").asText(""), null, null, firma));
            }
        }
    }

    // ===== Cuerpo de la petición en el formato REST de Gemini =====
    private Map<String, Object> construirCuerpo(SolicitudGemini solicitud) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("systemInstruction", Map.of("parts", List.of(Map.of("text", solicitud.instruccionSistema()))));

        List<Map<String, Object>> contenidos = new ArrayList<>();
        for (MensajeGemini mensaje : solicitud.contenidos()) {
            List<Map<String, Object>> partes = mensaje.partes().stream().map(this::convertirParte).toList();
            contenidos.add(Map.of("role", mensaje.rol(), "parts", partes));
        }
        cuerpo.put("contents", contenidos);

        if (solicitud.declaracionesHerramientas() != null && !solicitud.declaracionesHerramientas().isEmpty()) {
            cuerpo.put("tools", List.of(Map.of("functionDeclarations", solicitud.declaracionesHerramientas())));
            cuerpo.put("toolConfig", Map.of("functionCallingConfig",
                    Map.of("mode", solicitud.herramientasHabilitadas() ? "AUTO" : "NONE")));
        }
        cuerpo.put("generationConfig", Map.of("temperature", TEMPERATURA));
        return cuerpo;
    }

    private Map<String, Object> convertirParte(ParteGemini parte) {
        Map<String, Object> json = new LinkedHashMap<>();
        if (parte.llamada() != null) {
            json.put("functionCall", Map.of(
                    "name", parte.llamada().nombre(),
                    "args", parte.llamada().argumentos() == null ? Map.of() : parte.llamada().argumentos()));
        } else if (parte.respuesta() != null) {
            json.put("functionResponse", Map.of(
                    "name", parte.respuesta().nombre(),
                    "response", parte.respuesta().contenido()));
        } else {
            json.put("text", parte.texto() == null ? "" : parte.texto());
        }
        if (parte.firmaPensamiento() != null) {
            json.put("thoughtSignature", parte.firmaPensamiento());
        }
        return json;
    }

    private String recortar(String texto) {
        return texto.length() > 500 ? texto.substring(0, 500) + "..." : texto;
    }
}
