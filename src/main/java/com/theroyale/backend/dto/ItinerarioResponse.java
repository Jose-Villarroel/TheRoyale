package com.theroyale.backend.dto;

import java.util.List;

public record ItinerarioResponse(
        String titulo,
        String resumen,
        String estilo,
        int totalDias,
        List<DiaItinerarioDTO> dias
) {
    public record DiaItinerarioDTO(
            int numeroDia,
            String tituloDia,
            String manana,
            String tarde,
            String noche,
            String recomendacionHotel
    ) {}
}
