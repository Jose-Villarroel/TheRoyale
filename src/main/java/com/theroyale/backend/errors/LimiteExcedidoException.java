package com.theroyale.backend.errors;

// ===== Error: un cliente superó el límite permitido en una ventana de tiempo =====
// ApiExceptionHandler la traduce a 429 en JSON. La usan el chat (mensajes por IP) y el plan de
// visita (generaciones por día) para proteger la cuota de Gemini.
public class LimiteExcedidoException extends ExcepcionConCodigo {

    public LimiteExcedidoException(String mensaje) {
        super(mensaje);
    }

    private LimiteExcedidoException(String codigo, Object[] args) {
        super(codigo, args, null);
    }

    public static LimiteExcedidoException conCodigo(String codigo, Object... args) {
        return new LimiteExcedidoException(codigo, args);
    }
}
