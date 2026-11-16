package com.theroyale.backend.dto;

import com.theroyale.backend.model.Cliente;
import java.time.LocalDate;

public record ClienteResponseDto(
        Long id,
        String nombre,
        String apellido,
        String email,
        String cedula,
        String telefono,
        LocalDate fechaRegistro
) {
    public static ClienteResponseDto from(Cliente cliente) {
        if (cliente == null) return null;
        return new ClienteResponseDto(
                cliente.getId(),
                cliente.getNombre(),
                cliente.getApellido(),
                cliente.getEmail(),
                cliente.getCedula(),
                cliente.getTelefono(),
                cliente.getFechaRegistro()
        );
    }
}
