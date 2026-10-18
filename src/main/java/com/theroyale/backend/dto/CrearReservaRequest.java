package com.theroyale.backend.dto;

import java.time.LocalDate;

// ===== Datos que envía el frontend para crear una reserva =====
// El cliente elige un TIPO de habitación; la habitación concreta la asigna el sistema.
public record CrearReservaRequest(
        Long clienteId,
        Long tipoHabitacionId,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        Integer cantidadPersonas
) {
}