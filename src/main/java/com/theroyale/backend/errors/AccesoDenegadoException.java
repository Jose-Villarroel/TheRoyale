package com.theroyale.backend.errors;

// ===== Error de negocio: el usuario intenta actuar sobre un recurso que no es suyo =====
// Ej.: reseñar una reserva ajena o editar la reseña de otro cliente (RN-2, RN-7).
// ApiExceptionHandler la traduce a 403 en JSON.
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
