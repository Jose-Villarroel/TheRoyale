package com.theroyale.backend.dto;

import java.util.List;

// ===== Evento SSE "tarjetas" con platos de la carta: {"tipo":"PLATO","platos":[...]} =====
public record TarjetasPlatoChatResponse(
        String tipo,
        List<PlatoMenuResponse> platos
) {

    public static TarjetasPlatoChatResponse de(List<PlatoMenuResponse> platos) {
        return new TarjetasPlatoChatResponse("PLATO", platos);
    }
}
