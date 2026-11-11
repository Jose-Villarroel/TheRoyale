package com.theroyale.backend.service.plan.fuentes;

// ===== Error de una fuente externa del plan (sin clave, HTTP no exitoso o respuesta ilegible) =====
// No llega al huésped: ContextoPlanService la registra y agrega la fuente a fuentesNoDisponibles.
public class FuenteNoDisponibleException extends RuntimeException {

    public FuenteNoDisponibleException(String mensaje) {
        super(mensaje);
    }

    public FuenteNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
