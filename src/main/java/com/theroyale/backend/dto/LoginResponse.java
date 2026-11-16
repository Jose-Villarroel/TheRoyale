package com.theroyale.backend.dto;

// ===== Respuesta de autenticación para el frontend y clientes de API =====
public record LoginResponse(
        String tipo,
        Long id,
        String nombre,
        String apellido,
        String email,
        String token
) {
    public LoginResponse(String tipo, Long id, String nombre, String apellido, String email) {
        this(tipo, id, nombre, apellido, email, null);
    }
}
