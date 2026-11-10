package com.theroyale.backend.dto;

import java.util.List;

public record ChatResponse(
        String respuesta,
        List<String> sugerencias,
        String enlaceAccion,
        String textoAccion
) {}
