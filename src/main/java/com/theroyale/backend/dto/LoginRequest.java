package com.theroyale.backend.dto;

// ===== Cuerpo JSON que manda auth.service.ts en POST /api/auth/login =====
public record LoginRequest(String email, String password) {
}
