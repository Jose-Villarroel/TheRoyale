package com.theroyale.backend.dto.dashboard;

import java.math.BigDecimal;

// ===== Indicadores del hotel para un período, tal como los pinta la fila de KPI del dashboard =====
// Un mismo record sirve para el período consultado y para el anterior equivalente: la variación
// porcentual se calcula en Angular comparando los dos, así el backend no repite cada cifra dos veces.
//
// Las definiciones de cada cifra están en DashboardService (reglas RN-1 a RN-15): aquí solo viajan.
// "facturado" (ingresosTotales) y "cobrado" van siempre separados, nunca sumados ni intercambiados.
public record KpisResponse(
        BigDecimal ocupacionPorcentaje,
        BigDecimal adr,
        BigDecimal revpar,
        BigDecimal ingresosHabitaciones,
        BigDecimal ingresosServicios,
        BigDecimal ingresosTotales,
        BigDecimal cobrado,
        BigDecimal saldoPendiente,
        long nochesVendidas,
        long habitacionesDisponibles,
        long habitacionesMantenimiento,
        int diasPeriodo,
        BigDecimal ratingPromedio,
        long totalResenas,
        BigDecimal tasaCancelacion,
        BigDecimal leadTimeDias,
        BigDecimal alosNoches,
        BigDecimal ingresoPorHuesped,
        long reservasAtendidas
) {
}
