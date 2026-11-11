package com.theroyale.backend.dto.plan;

// ===== Cuerpo de POST /plan/dias/{fecha}/regenerar: indicación opcional de hasta 200 caracteres =====
public record RegenerarDiaRequest(
        Long clienteId,
        String indicacion
) {
}
