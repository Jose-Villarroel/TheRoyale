package com.theroyale.backend.dto;

import java.math.BigDecimal;

// ===== Disponibilidad de un tipo de habitación en un rango de fechas =====
// precioDesde es el precio más bajo entre las habitaciones libres; null si no hay ninguna.
public record DisponibilidadTipoResponse(
        Long tipoHabitacionId,
        String nombre,
        String descripcion,
        String imagenUrl,
        int habitacionesDisponibles,
        BigDecimal precioDesde
) {
}
