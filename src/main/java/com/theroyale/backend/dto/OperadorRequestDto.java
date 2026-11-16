package com.theroyale.backend.dto;

import com.theroyale.backend.model.TipoOperador;

public record OperadorRequestDto(
        String nombre,
        String email,
        String password,
        TipoOperador tipo
) {
}
