package com.theroyale.backend.service.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.theroyale.backend.config.GeminiProperties;
import com.theroyale.backend.service.chat.GeminiClient.MensajeGemini;
import com.theroyale.backend.service.chat.GeminiClient.ParteGemini;
import com.theroyale.backend.service.chat.GeminiClient.SolicitudGemini;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// ===== Pruebas de GeminiRestClient contra un servidor HTTP local que imita a Gemini =====
// No sale a internet: se comprueba la URL, el header de la clave, el cuerpo JSON enviado
// (systemInstruction, contents, tools, toolConfig) y la lectura del stream "data: {...}".
class GeminiRestClientTest {

    private HttpServer servidor;
    private GeminiRestClient cliente;
    private int estadoRespuesta;
    private String cuerpoRespuesta;
    private String rutaRecibida;
    private String claveRecibida;
    private String cuerpoRecibido;
    private final List<String> rutasRecibidas = new ArrayList<>();
    // Si la ruta pedida contiene este modelo, el servidor falso responde 503 (modelo saturado)
    private String modeloQueFalla;
    private GeminiProperties propiedades;

    @BeforeEach
    void setUp() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/", intercambio -> {
            rutaRecibida = intercambio.getRequestURI().toString();
            rutasRecibidas.add(rutaRecibida);
            claveRecibida = intercambio.getRequestHeaders().getFirst("x-goog-api-key");
            cuerpoRecibido = new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            boolean falla = modeloQueFalla != null && rutaRecibida.contains("/models/" + modeloQueFalla + ":");
            int estado = falla ? 503 : estadoRespuesta;
            String cuerpo = falla ? "{\"error\":{\"code\":503,\"message\":\"The model is overloaded.\"}}" : cuerpoRespuesta;
            byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type",
                    estado == 200 ? "text/event-stream" : "application/json");
            intercambio.sendResponseHeaders(estado, bytes.length);
            try (OutputStream salida = intercambio.getResponseBody()) {
                salida.write(bytes);
            }
        });
        servidor.start();

        propiedades = new GeminiProperties();
        propiedades.setApiKey("clave-falsa-de-prueba");
        cliente = new GeminiRestClient(propiedades, new ObjectMapper(),
                "http://localhost:" + servidor.getAddress().getPort() + "/v1beta");
    }

    @AfterEach
    void tearDown() {
        servidor.stop(0);
    }

    @Test
    void geminiRestClient_transmitir_leeTextoYFunctionCallIgnorandoPensamientos() {
        // Arrange
        estadoRespuesta = 200;
        cuerpoRespuesta = """
                data: {"candidates":[{"content":{"role":"model","parts":[{"text":"Hello "}]}}]}

                data: {"candidates":[{"content":{"role":"model","parts":[{"text":"secret reasoning","thought":true},\
                {"functionCall":{"name":"buscar_platos","args":{"etiqueta":"VEGANO"}},"thoughtSignature":"firma-123"}]}}]}

                """;
        List<ParteGemini> partes = new ArrayList<>();

        // Act
        cliente.transmitir(solicitudSimple(true), partes::add);

        // Assert
        assertThat(partes).hasSize(2);
        assertThat(partes.get(0).texto()).isEqualTo("Hello ");
        assertThat(partes.get(1).llamada().nombre()).isEqualTo("buscar_platos");
        assertThat(partes.get(1).llamada().argumentos()).isEqualTo(Map.of("etiqueta", "VEGANO"));
        assertThat(partes.get(1).firmaPensamiento()).isEqualTo("firma-123");
        assertThat(rutaRecibida).isEqualTo("/v1beta/models/gemini-3.1-flash-lite:streamGenerateContent?alt=sse");
        assertThat(claveRecibida).isEqualTo("clave-falsa-de-prueba");
    }

    @Test
    void geminiRestClient_transmitir_enviaElCuerpoEnElFormatoDeGemini() throws IOException {
        // Arrange
        estadoRespuesta = 200;
        cuerpoRespuesta = "data: {\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"ok\"}]}}]}\n\n";
        ParteGemini llamada = new ParteGemini(null,
                new GeminiClient.LlamadaFuncion("consultar_disponibilidad", Map.of("fechaInicio", "2026-12-01")),
                null, "firma-xyz");
        SolicitudGemini solicitud = new SolicitudGemini("SYSTEM",
                List.of(new MensajeGemini("user", List.of(ParteGemini.deTexto("Hi"))),
                        new MensajeGemini("model", List.of(llamada)),
                        new MensajeGemini("user", List.of(
                                ParteGemini.deRespuesta("consultar_disponibilidad", Map.of("noches", 3))))),
                List.of(Map.of("name", "consultar_disponibilidad")), false);

        // Act
        cliente.transmitir(solicitud, parte -> { });

        // Assert
        JsonNode cuerpo = new ObjectMapper().readTree(cuerpoRecibido);
        assertThat(cuerpo.at("/systemInstruction/parts/0/text").asText()).isEqualTo("SYSTEM");
        assertThat(cuerpo.at("/contents/0/role").asText()).isEqualTo("user");
        assertThat(cuerpo.at("/contents/0/parts/0/text").asText()).isEqualTo("Hi");
        assertThat(cuerpo.at("/contents/1/role").asText()).isEqualTo("model");
        assertThat(cuerpo.at("/contents/1/parts/0/functionCall/name").asText()).isEqualTo("consultar_disponibilidad");
        assertThat(cuerpo.at("/contents/1/parts/0/thoughtSignature").asText()).isEqualTo("firma-xyz");
        assertThat(cuerpo.at("/contents/2/parts/0/functionResponse/response/noches").asInt()).isEqualTo(3);
        assertThat(cuerpo.at("/tools/0/functionDeclarations/0/name").asText()).isEqualTo("consultar_disponibilidad");
        assertThat(cuerpo.at("/toolConfig/functionCallingConfig/mode").asText()).isEqualTo("NONE");
    }

    @Test
    void geminiRestClient_transmitir_respuestaDeError_lanzaGeminiException() {
        // Arrange
        estadoRespuesta = 400;
        cuerpoRespuesta = "{\"error\":{\"code\":400,\"message\":\"API key not valid.\"}}";

        // Act & Assert
        assertThatThrownBy(() -> cliente.transmitir(solicitudSimple(true), parte -> { }))
                .isInstanceOf(GeminiException.class)
                .hasMessageContaining("400");
    }

    @Test
    void geminiRestClient_transmitir_consultaBloqueada_lanzaGeminiException() {
        // Arrange
        estadoRespuesta = 200;
        cuerpoRespuesta = "data: {\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}\n\n";

        // Act & Assert
        assertThatThrownBy(() -> cliente.transmitir(solicitudSimple(true), parte -> { }))
                .isInstanceOf(GeminiException.class)
                .hasMessageContaining("SAFETY");
    }

    // ===== Modelos de respaldo: Google a veces retira o satura un modelo =====
    @Test
    void geminiRestClient_transmitir_principalFallaSinResponder_usaElModeloDeRespaldo() {
        // Arrange: el principal responde 503 y el respaldo contesta bien
        propiedades.setModelo("modelo-principal");
        propiedades.setModelosRespaldo(List.of("modelo-respaldo"));
        modeloQueFalla = "modelo-principal";
        estadoRespuesta = 200;
        cuerpoRespuesta = "data: {\"candidates\":[{\"content\":{\"role\":\"model\",\"parts\":[{\"text\":\"Hello\"}]}}]}\n\n";
        List<ParteGemini> partes = new ArrayList<>();

        // Act
        cliente.transmitir(solicitudSimple(false), partes::add);

        // Assert: se intentaron los dos en orden y la respuesta llegó una sola vez
        assertThat(rutasRecibidas).hasSize(2);
        assertThat(rutasRecibidas.get(0)).contains("/models/modelo-principal:");
        assertThat(rutasRecibidas.get(1)).contains("/models/modelo-respaldo:");
        assertThat(partes).extracting(ParteGemini::texto).containsExactly("Hello");
    }

    @Test
    void geminiRestClient_transmitir_fallaDespuesDeEmpezarAResponder_noReintentaConOtroModelo() {
        // Arrange: el principal entrega un fragmento y luego manda una línea corrupta
        propiedades.setModelo("modelo-principal");
        propiedades.setModelosRespaldo(List.of("modelo-respaldo"));
        estadoRespuesta = 200;
        cuerpoRespuesta = "data: {\"candidates\":[{\"content\":{\"role\":\"model\",\"parts\":[{\"text\":\"Hel\"}]}}]}\n\n"
                + "data: {esto no es json\n\n";
        List<ParteGemini> partes = new ArrayList<>();

        // Act & Assert: reintentar duplicaría el texto ya mostrado, así que se propaga el error
        assertThatThrownBy(() -> cliente.transmitir(solicitudSimple(false), partes::add))
                .isInstanceOf(GeminiException.class);
        assertThat(rutasRecibidas).hasSize(1);
        assertThat(partes).extracting(ParteGemini::texto).containsExactly("Hel");
    }

    private SolicitudGemini solicitudSimple(boolean herramientas) {
        return new SolicitudGemini("SYSTEM",
                List.of(new MensajeGemini("user", List.of(ParteGemini.deTexto("Hi")))),
                List.of(), herramientas);
    }
}
