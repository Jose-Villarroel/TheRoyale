package com.theroyale.backend.dto;

import java.math.BigDecimal;

// ===== Pago registrado contra la cuenta de una reserva (AC25) =====
public record RegistrarPagoRequest(
        BigDecimal monto,
        String metodoPago,
        Long operadorId
) {
}
