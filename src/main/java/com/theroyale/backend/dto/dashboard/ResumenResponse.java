package com.theroyale.backend.dto.dashboard;

import java.time.LocalDate;
import java.time.LocalDateTime;

// ===== Respuesta de GET /api/admin/dashboard/resumen =====
// Lleva el período pedido y el anterior equivalente (mismo número de dias justo antes de "desde"),
// porque cada tarjeta de KPI muestra la variacion frente a él.
// generadoEn alimenta la hora de última actualización del encabezado.
public record ResumenResponse(
        LocalDate desde,
        LocalDate hasta,
        LocalDate desdeAnterior,
        LocalDate hastaAnterior,
        KpisResponse actual,
        KpisResponse anterior,
        LocalDateTime generadoEn
) {
}
