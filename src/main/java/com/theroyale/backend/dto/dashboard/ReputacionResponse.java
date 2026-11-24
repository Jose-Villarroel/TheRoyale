package com.theroyale.backend.dto.dashboard;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

// ===== Respuesta de GET /api/admin/dashboard/reputacion =====
// Va aparte de /desglose porque consulta Resena y Cliente, no ingresos: mezclarlas obligaría a
// recalcular las reseñas cada vez que el panel cambia el período de ingresos.
// distribucion va de la estrella 1 a la 5, con 0 en las que nadie puso: el gráfico necesita las cinco.
public record ReputacionResponse(
        BigDecimal promedio,
        long total,
        Map<Integer, Long> distribucion,
        List<RatingTipoResponse> porTipo,
        List<ResenaRecienteResponse> recientes,
        long reservasClientesNuevos,
        long reservasClientesRecurrentes,
        BigDecimal leadTimeDias
) {
}
