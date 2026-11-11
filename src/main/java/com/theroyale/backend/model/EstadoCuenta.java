package com.theroyale.backend.model;

// ===== Estado de la cuenta de consumo de una reserva =====
// ABIERTA admite consumos y pagos; pasa a CERRADA en el check-out o al cancelar la reserva.
public enum EstadoCuenta {
    ABIERTA, CERRADA
}
