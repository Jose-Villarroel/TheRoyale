package com.theroyale.backend.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

// ===== Un día de la serie de ocupación =====
// futuro = true en los días posteriores a hoy: son reservas ya tomadas ("on the books"), no hechos
// consumados, y la gráfica los sombrea para que no se lean como ocupación real.
//
// Lleva también el ingreso de habitación del día porque la consulta ya lo calcula: con eso el panel
// dibuja las sparklines de ADR y RevPAR a partir de datos reales, en vez de pedir otra serie.
public record PuntoOcupacionResponse(
        LocalDate fecha,
        long nochesVendidas,
        BigDecimal ingresosHabitaciones,
        long habitacionesDisponibles,
        BigDecimal ocupacionPorcentaje,
        boolean futuro
) {
}
