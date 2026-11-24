package com.theroyale.backend.dto.dashboard;

import java.math.BigDecimal;

// ===== Cuenta abierta con saldo a favor del hotel =====
// Alimenta a la vez el contador de check-outs con saldo y el detalle de la alerta, para que el
// administrador vea a quién hay que cobrarle sin abrir otra pantalla.
public record SaldoPendienteResponse(
        Long reservaId,
        String habitacionNumero,
        String cliente,
        BigDecimal saldo
) {
}
