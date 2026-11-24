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
        BigDecimal precioDesde,
        // "High season", "High demand" o "Special rate" si el admin aprobó un precio especial para
        // alguna noche de esas fechas; null con el precio base
        String etiquetaPrecio
) {

    public DisponibilidadTipoResponse(Long tipoHabitacionId, String nombre, String descripcion, String imagenUrl,
                                      int habitacionesDisponibles, BigDecimal precioDesde) {
        this(tipoHabitacionId, nombre, descripcion, imagenUrl, habitacionesDisponibles, precioDesde, null);
    }
}
