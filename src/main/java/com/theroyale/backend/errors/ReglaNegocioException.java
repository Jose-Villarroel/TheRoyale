package com.theroyale.backend.errors;

import org.springframework.http.HttpStatus;

// ===== Regla de negocio incumplida, con su código y su HTTP =====
// Reemplaza a los IllegalArgumentException (400) e IllegalStateException (409) de los sitios que
// alcanza un huésped: esos solo llevaban un literal y el estado salía del tipo de la excepción.
// Aquí el estado viaja en la propia excepción, así que un mismo tipo cubre 400, 409 y los demás
// (el plan ilegible es 502 y el límite diario, 429, aunque ese tiene su propia excepción).
//
// Siempre lleva código: para un literal sin código quedan los IllegalArgumentException /
// IllegalStateException de siempre, que ApiExceptionHandler sigue traduciendo a 400 y 409.
public class ReglaNegocioException extends ExcepcionConCodigo {

    private final HttpStatus estado;

    private ReglaNegocioException(HttpStatus estado, String codigo, Object[] args, Throwable causa) {
        super(codigo, args, causa);
        this.estado = estado;
    }

    public static ReglaNegocioException badRequest(String codigo, Object... args) {
        return new ReglaNegocioException(HttpStatus.BAD_REQUEST, codigo, args, null);
    }

    public static ReglaNegocioException conflict(String codigo, Object... args) {
        return new ReglaNegocioException(HttpStatus.CONFLICT, codigo, args, null);
    }

    public static ReglaNegocioException de(HttpStatus estado, String codigo, Object... args) {
        return new ReglaNegocioException(estado, codigo, args, null);
    }

    public static ReglaNegocioException de(HttpStatus estado, Throwable causa, String codigo, Object... args) {
        return new ReglaNegocioException(estado, codigo, args, causa);
    }

    public HttpStatus getEstado() {
        return estado;
    }
}
