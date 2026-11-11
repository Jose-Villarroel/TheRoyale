package com.theroyale.backend.errors;

// ===== Error: un cliente superó el límite de mensajes permitido en una ventana de tiempo =====
// ApiExceptionHandler la traduce a 429 en JSON. La usa el chat para proteger la cuota de Gemini.
public class LimiteExcedidoException extends RuntimeException {

    public LimiteExcedidoException(String mensaje) {
        super(mensaje);
    }
}
