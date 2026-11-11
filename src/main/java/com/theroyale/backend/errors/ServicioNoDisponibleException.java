package com.theroyale.backend.errors;

// ===== Error: una funcionalidad depende de un servicio externo que no está configurado o no responde =====
// ApiExceptionHandler la traduce a 503 en JSON (por ejemplo, el chat sin GEMINI_API_KEY).
public class ServicioNoDisponibleException extends RuntimeException {

    public ServicioNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
