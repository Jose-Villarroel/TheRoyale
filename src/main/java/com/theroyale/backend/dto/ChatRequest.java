package com.theroyale.backend.dto;

import java.util.List;

// ===== Mensaje que el widget del chat envía a POST /api/chat =====
// El backend no guarda conversaciones: el front manda en cada mensaje los turnos anteriores
// (historial), de los que solo se usan los últimos gemini.max-turnos.
public record ChatRequest(
        String mensaje,
        List<TurnoChatRequest> historial
) {
}
