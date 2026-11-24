package com.theroyale.backend.errors;

// ===== Error de negocio: el usuario intenta actuar sobre un recurso que no es suyo =====
// Ej.: reseñar una reserva ajena o editar la reseña de otro cliente (RN-2, RN-7).
// ApiExceptionHandler la traduce a 403 en JSON.
public class AccesoDenegadoException extends ExcepcionConCodigo {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }

    private AccesoDenegadoException(String codigo, Object[] args) {
        super(codigo, args, null);
    }

    public static AccesoDenegadoException conCodigo(String codigo, Object... args) {
        return new AccesoDenegadoException(codigo, args);
    }
}
