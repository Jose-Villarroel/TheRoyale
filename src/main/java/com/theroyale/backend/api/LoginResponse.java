package com.theroyale.backend.api;

// ===== Respuesta que espera la interfaz LoginResponse de auth.service.ts =====
// No corresponde a ninguna entidad: Cliente no tiene "tipo" y Operador no tiene "apellido".
public record LoginResponse(String tipo, Long id, String nombre, String apellido, String email) {
}
