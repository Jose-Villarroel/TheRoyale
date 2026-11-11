package com.theroyale.backend.service.chat;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

// ===== Puerto hacia la API de Gemini (streamGenerateContent) =====
// Es una interfaz para que las pruebas usen un Gemini falso: ninguna prueba llama a la API real.
// La implementación de producción es GeminiRestClient.
public interface GeminiClient {

    // Envía la conversación y entrega cada parte de la respuesta (texto o llamada a herramienta)
    // a medida que llega. Bloquea hasta que Gemini termina; si falla lanza GeminiException.
    void transmitir(SolicitudGemini solicitud, Consumer<ParteGemini> alRecibirParte);

    // herramientasHabilitadas = false obliga al modelo a responder solo con texto (última ronda)
    record SolicitudGemini(
            String instruccionSistema,
            List<MensajeGemini> contenidos,
            List<Map<String, Object>> declaracionesHerramientas,
            boolean herramientasHabilitadas
    ) {
    }

    // rol en el formato de Gemini: "user" o "model"
    record MensajeGemini(String rol, List<ParteGemini> partes) {
    }

    // Exactamente uno de texto, llamada o respuesta viene informado.
    // firmaPensamiento (thoughtSignature) se devuelve tal cual a Gemini en la ronda siguiente.
    record ParteGemini(String texto, LlamadaFuncion llamada, RespuestaFuncion respuesta, String firmaPensamiento) {

        public static ParteGemini deTexto(String texto) {
            return new ParteGemini(texto, null, null, null);
        }

        public static ParteGemini deLlamada(String nombre, Map<String, Object> argumentos) {
            return new ParteGemini(null, new LlamadaFuncion(nombre, argumentos), null, null);
        }

        public static ParteGemini deRespuesta(String nombre, Map<String, Object> contenido) {
            return new ParteGemini(null, null, new RespuestaFuncion(nombre, contenido), null);
        }
    }

    record LlamadaFuncion(String nombre, Map<String, Object> argumentos) {
    }

    record RespuestaFuncion(String nombre, Map<String, Object> contenido) {
    }
}
