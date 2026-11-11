package com.theroyale.backend.dto;

import com.theroyale.backend.model.EstadoHabitacion;

// ===== Datos que envía el frontend para crear o editar una habitación =====
// El tipo llega solo como id; HabitacionService lo resuelve contra la base antes de guardar.
public record HabitacionRequest(
        String numero,
        Double precio,
        EstadoHabitacion estado,
        Long tipoHabitacionId
) {
}
