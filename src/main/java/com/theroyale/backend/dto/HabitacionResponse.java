package com.theroyale.backend.dto;

import com.theroyale.backend.model.EstadoHabitacion;
import com.theroyale.backend.model.Habitacion;

// ===== Habitación tal como la consume el frontend Angular =====
// Plana a propósito: del tipo de habitación solo viajan su id y su nombre, en lugar del
// objeto TipoHabitacion anidado que devolvería la entidad serializada directamente.
public record HabitacionResponse(
        Long id,
        String numero,
        Double precio,
        EstadoHabitacion estado,
        Long tipoHabitacionId,
        String tipoHabitacionNombre
) {

    public static HabitacionResponse from(Habitacion h) {
        return new HabitacionResponse(
                h.getId(),
                h.getNumero(),
                h.getPrecio(),
                h.getEstado(),
                h.getTipoHabitacion() == null ? null : h.getTipoHabitacion().getId(),
                h.getTipoHabitacion() == null ? null : h.getTipoHabitacion().getNombre());
    }
}
