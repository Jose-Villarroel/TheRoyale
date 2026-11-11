package com.theroyale.backend.dto;

// ===== GET /api/chat/estado: si el chat tiene clave de Gemini configurada =====
// Permite al widget mostrar el aviso "offline" antes de que el cliente escriba.
public record EstadoChatResponse(
        boolean disponible
) {
}
