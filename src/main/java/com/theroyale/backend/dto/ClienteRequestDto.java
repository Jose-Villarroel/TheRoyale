package com.theroyale.backend.dto;

public record ClienteRequestDto(
        String nombre,
        String apellido,
        String email,
        String password,
        String cedula,
        String telefono
) {
}
