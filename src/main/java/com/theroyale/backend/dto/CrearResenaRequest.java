package com.theroyale.backend.dto;

public record CrearResenaRequest(
        Long clienteId,
        Long tipoHabitacionId,
        int estrellas,
        String titulo,
        String comentario
) {}
