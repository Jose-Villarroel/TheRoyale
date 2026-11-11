package com.theroyale.backend.dto.plan;

// ===== Cuerpo de POST /api/reservas/{id}/plan/interpretar: el viaje descrito en texto libre =====
public record InterpretarPlanRequest(
        Long clienteId,
        String descripcion
) {
}
