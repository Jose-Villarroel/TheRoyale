package com.theroyale.backend.model;

// ===== Ciclo de vida de una reserva y su equivalencia con el enunciado del proyecto =====
// PENDIENTE  -> reserva "inactiva": la estadía aún no empieza. Toda reserva nace PENDIENTE.
// CONFIRMADA -> también "inactiva": el hotel la aceptó y se abre su cuenta, pero no hay check-in.
// EN_CURSO   -> reserva "activa": el huésped hizo check-in y está en la habitación.
// FINALIZADA -> estadía terminada (check-out); solo se permite con saldo 0 en la cuenta.
// CANCELADA  -> anulada antes de la llegada; libera la habitación para esas fechas.
public enum EstadoReserva {
    PENDIENTE, CONFIRMADA, EN_CURSO, CANCELADA, FINALIZADA
}
