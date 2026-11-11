package com.theroyale.backend.dto;

import java.util.Map;

// ===== Resumen de las reseñas visibles de un tipo de habitación =====
// promedio redondeado a 1 decimal, o null si no hay reseñas visibles (total = 0) (RN-10).
// distribucion: cantidad de reseñas por estrellas; las claves 1..5 siempre están, con 0 si no hay.
public record ResumenResenasResponse(
        Long tipoHabitacionId,
        Double promedio,
        long total,
        Map<Integer, Long> distribucion
) {
}
