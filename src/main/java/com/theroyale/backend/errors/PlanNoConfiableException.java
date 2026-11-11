package com.theroyale.backend.errors;

// ===== Error: la IA devolvió dos veces un plan con demasiados bloques inválidos (más del 40 %) =====
// ApiExceptionHandler la traduce a 502: el fallo es del servicio externo (Gemini), no del huésped.
public class PlanNoConfiableException extends RuntimeException {

    public static final String MENSAJE = "We could not build a reliable plan. Please try again.";

    public PlanNoConfiableException() {
        super(MENSAJE);
    }
}
