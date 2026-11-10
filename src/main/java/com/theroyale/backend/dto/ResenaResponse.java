package com.theroyale.backend.dto;

import com.theroyale.backend.model.Resena;

import java.time.format.DateTimeFormatter;

public record ResenaResponse(
        Long id,
        Long clienteId,
        String clienteNombre,
        String clienteApellido,
        Long tipoHabitacionId,
        String tipoHabitacionNombre,
        int estrellas,
        String titulo,
        String comentario,
        String fecha
) {
    public static ResenaResponse from(Resena r) {
        return new ResenaResponse(
                r.getId(),
                r.getCliente().getId(),
                r.getCliente().getNombre(),
                r.getCliente().getApellido(),
                r.getTipoHabitacion().getId(),
                r.getTipoHabitacion().getNombre(),
                r.getEstrellas(),
                r.getTitulo(),
                r.getComentario(),
                r.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        );
    }
}
