package com.theroyale.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

// ===== Tarjeta de un tipo de habitación libre que el chat muestra bajo la respuesta =====
// La arma el backend con el resultado de consultar_disponibilidad (nunca el modelo), así los
// botones "View room" y "Book this room" siempre apuntan a un tipo y unas fechas reales.
// promedio es null si el tipo no tiene reseñas visibles.
public record TarjetaHabitacionChat(
        Long tipoHabitacionId,
        String nombre,
        String descripcion,
        String imagenUrl,
        BigDecimal precioDesde,
        int habitacionesDisponibles,
        Double promedio,
        long totalResenas,
        LocalDate fechaInicio,
        LocalDate fechaFin
) {
}
