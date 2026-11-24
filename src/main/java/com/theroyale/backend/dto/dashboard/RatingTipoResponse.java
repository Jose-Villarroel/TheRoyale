package com.theroyale.backend.dto.dashboard;

import java.math.BigDecimal;

// ===== Calificación promedio de un tipo de habitación =====
public record RatingTipoResponse(
        Long tipoHabitacionId,
        String nombre,
        BigDecimal promedio,
        long total
) {
}
