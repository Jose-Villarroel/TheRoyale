package com.theroyale.backend.dto;

import java.util.List;
import java.util.Map;

public record ResumenResenasResponse(
        double promedioEstrellas,
        long totalResenas,
        Map<Integer, Long> conteoPorEstrellas,
        List<ResenaResponse> resenas
) {}
