package com.theroyale.backend.errors;

// ===== Error de negocio: se pidió un registro (por id, número o nombre) que no existe =====
// ApiExceptionHandler la traduce a 404 en JSON y GlobalExceptionHandler a la página de error.
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
