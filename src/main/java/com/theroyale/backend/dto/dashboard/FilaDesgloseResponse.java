package com.theroyale.backend.dto.dashboard;

import java.math.BigDecimal;

// ===== Una fila de cualquier desglose (tipo de habitación, servicio, método de pago, operador) =====
// Un solo record para los cuatro desgloses: todos son "etiqueta + importe + unidades", y así las
// barras horizontales del dashboard se dibujan con un único componente.
// porcentaje viene calculado del servidor sobre el total del desglose, para que la barra no tenga
// que conocer las demás filas.
public record FilaDesgloseResponse(
        String etiqueta,
        BigDecimal valor,
        long cantidad,
        BigDecimal porcentaje
) {
}
