package com.theroyale.backend.dto.dashboard;

import java.time.LocalDateTime;

// ===== Reseña reciente mostrada en el bloque de reputación (solo las visibles) =====
public record ResenaRecienteResponse(
        Long id,
        int calificacion,
        String titulo,
        String comentario,
        String cliente,
        String tipoHabitacion,
        LocalDateTime fechaCreacion
) {
}
