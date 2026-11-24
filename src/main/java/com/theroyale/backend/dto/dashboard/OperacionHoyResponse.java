package com.theroyale.backend.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// ===== Respuesta de GET /api/admin/dashboard/hoy: la operación del día =====
// Es la sección más urgente del panel. Incluye el mapa de habitaciones porque es estado actual
// y no una serie, así la pantalla lo pide en la misma petición que el resto de la operación.
public record OperacionHoyResponse(
        LocalDate fecha,
        long llegadas,
        long salidas,
        long enCasa,
        long salidasConSaldo,
        BigDecimal saldoPendienteTotal,
        long habitacionesMantenimiento,
        long habitacionesDeshabilitadas,
        List<HabitacionMapaResponse> habitaciones,
        List<SaldoPendienteResponse> detalleSaldos
) {
}
