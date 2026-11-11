package com.theroyale.backend.dto;

// ===== Carga de un servicio a la cuenta de una reserva (AC23) =====
// operadorId identifica a quien registra el cargo; si falta, CuentaService usa el predeterminado.
public record AgregarConsumoRequest(
        Long servicioId,
        Integer cantidad,
        Long operadorId
) {
}
