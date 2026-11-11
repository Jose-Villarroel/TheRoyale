package com.theroyale.backend.service.chat;

// ===== Error al hablar con Gemini (HTTP no exitoso, respuesta bloqueada o corte de la conexión) =====
// ChatService la convierte en el evento SSE "error"; el detalle solo va al log, nunca al cliente.
public class GeminiException extends RuntimeException {

    public GeminiException(String mensaje) {
        super(mensaje);
    }

    public GeminiException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
