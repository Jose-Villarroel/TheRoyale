package com.theroyale.backend.dto.plan;

import java.util.List;

// ===== Cuerpo de POST /api/reservas/{id}/plan: genera el plan o reemplaza el que haya =====
// descripcion (texto libre, hasta 1000) y entendido son opcionales: llegan desde la ruta de texto libre.
public record GenerarPlanRequest(
        Long clienteId,
        PreferenciasPlan preferencias,
        String descripcion,
        List<EntendidoItem> entendido
) {
}
