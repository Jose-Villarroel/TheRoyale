package com.theroyale.backend.dto.dashboard;

import java.util.List;

// ===== Respuesta de GET /api/admin/dashboard/desglose =====
// Las cuatro listas van ordenadas de mayor a menor en el servidor: el panel las pinta tal cual.
public record DesgloseResponse(
        List<FilaDesgloseResponse> porTipoHabitacion,
        List<FilaDesgloseResponse> porServicio,
        List<FilaDesgloseResponse> porMetodoPago,
        List<FilaDesgloseResponse> porOperador
) {
}
