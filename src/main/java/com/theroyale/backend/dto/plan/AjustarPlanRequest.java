package com.theroyale.backend.dto.plan;

// ===== Cuerpo de POST /plan/ajustar: "Would you like something different?" (1 a 200 caracteres) =====
public record AjustarPlanRequest(
        Long clienteId,
        String indicacion
) {
}
