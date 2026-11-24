package com.theroyale.backend.errors;

// ===== Error de negocio: se pidió un registro (por id, número o nombre) que no existe =====
// ApiExceptionHandler la traduce a 404 en JSON y GlobalExceptionHandler a la página de error.
// conCodigo(...) es la variante del contrato (docs/i18n-codigos-error.md); el constructor de
// mensaje plano se queda para los recursos que solo alcanza el personal del hotel.
public class RecursoNoEncontradoException extends ExcepcionConCodigo {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }

    private RecursoNoEncontradoException(String codigo, Object[] args) {
        super(codigo, args, null);
    }

    public static RecursoNoEncontradoException conCodigo(String codigo, Object... args) {
        return new RecursoNoEncontradoException(codigo, args);
    }
}
