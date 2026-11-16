package com.theroyale.backend.dto;

import com.theroyale.backend.model.Operador;
import com.theroyale.backend.model.TipoOperador;

public record OperadorResponseDto(
        Long id,
        String nombre,
        String email,
        TipoOperador tipo
) {
    public static OperadorResponseDto from(Operador operador) {
        if (operador == null) return null;
        return new OperadorResponseDto(
                operador.getId(),
                operador.getNombre(),
                operador.getEmail(),
                operador.getTipo()
        );
    }
}
