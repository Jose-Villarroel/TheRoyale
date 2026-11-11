package com.theroyale.backend.service.plan;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

// ===== Plan tal como lo devuelve Gemini (esquema de la sección 7), antes de validarlo =====
// Nada de esto llega al huésped sin pasar por ValidadorPlanService: de la IA solo se conservan
// las horas, la elección de referenciaId y el motivo.
@JsonIgnoreProperties(ignoreUnknown = true)
public record PlanGenerado(
        String resumen,
        List<DiaGenerado> dias
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DiaGenerado(String fecha, String titulo, List<BloqueGenerado> bloques) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BloqueGenerado(String inicio, String fin, String tipo, String referenciaId,
                                 String caracteristica, String titulo, String motivo) {
    }

    // Respuesta de GeneradorPlanService: el plan y el modelo de Gemini que lo produjo
    public record Respuesta(PlanGenerado plan, String modelo) {
    }
}
