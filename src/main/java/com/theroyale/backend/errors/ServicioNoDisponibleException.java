package com.theroyale.backend.errors;

// ===== Error: una funcionalidad depende de un servicio externo que no está configurado o no responde =====
// ApiExceptionHandler la traduce a 503 en JSON (por ejemplo, el chat sin GEMINI_API_KEY).
public class ServicioNoDisponibleException extends ExcepcionConCodigo {

    public ServicioNoDisponibleException(String mensaje) {
        super(mensaje);
    }

    private ServicioNoDisponibleException(String codigo, Object[] args) {
        super(codigo, args, null);
    }

    public static ServicioNoDisponibleException conCodigo(String codigo, Object... args) {
        return new ServicioNoDisponibleException(codigo, args);
    }
}
