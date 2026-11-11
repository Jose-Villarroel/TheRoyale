package com.theroyale.backend.dto;

import java.util.List;

// ===== Evento SSE "tarjetas" con habitaciones: {"tipo":"HABITACION","habitaciones":[...]} =====
public record TarjetasHabitacionChatResponse(
        String tipo,
        List<TarjetaHabitacionChat> habitaciones
) {

    public static TarjetasHabitacionChatResponse de(List<TarjetaHabitacionChat> habitaciones) {
        return new TarjetasHabitacionChatResponse("HABITACION", habitaciones);
    }
}
