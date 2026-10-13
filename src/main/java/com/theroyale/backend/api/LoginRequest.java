package com.theroyale.backend.api;

// ===== Cuerpo JSON que manda auth.service.ts en POST /api/auth/login =====
public record LoginRequest(String email, String password) {
}
