package com.theroyale.backend.dto;

// ===== Un turno previo de la conversación: rol "usuario" o "asistente" y su texto =====
public record TurnoChatRequest(
        String rol,
        String texto
) {
}
