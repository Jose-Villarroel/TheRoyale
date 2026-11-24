package com.theroyale.backend.dto;

import java.time.LocalDate;

// ===== Alta o edición de una temporada de precios (admin) =====
public record TemporadaRequest(String nombre, LocalDate fechaInicio, LocalDate fechaFin, Double factor) {
}
