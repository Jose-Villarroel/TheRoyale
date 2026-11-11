package com.theroyale.backend.dto.plan;

// ===== Cuerpo de POST /plan/bloques/{bloqueId}/agregar-a-cuenta ("Add to my stay") =====
public record AgregarBloqueRequest(
        Long clienteId
) {
}
