package com.theroyale.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// ===== DTO integral para el Dashboard de Administración =====
public record DashboardResponse(
        double porcentajeOcupacion,
        long habitacionesOcupadas,
        long habitacionesDisponibles,
        long habitacionesMantenimiento,
        long totalHabitaciones,
        long llegadasHoy,
        long salidasHoy,
        long reservasActivas,
        long totalClientes,
        BigDecimal ingresosMesActual,
        BigDecimal ingresosTotales,
        List<OcupacionTipoDTO> ocupacionPorTipo,
        List<IngresoMensualDTO> ingresosMensuales,
        Map<String, Long> habitacionesPorEstado,
        List<ActividadRecienteDTO> actividadReciente,
        List<PrediccionOcupacionDTO> proyeccionOcupacion
) {
    public record OcupacionTipoDTO(
            Long tipoId,
            String tipoNombre,
            long total,
            long ocupadas,
            double porcentaje
    ) {}

    public record IngresoMensualDTO(
            String mes,
            String anioMes,
            BigDecimal ingresosHospedaje,
            BigDecimal ingresosServicios,
            BigDecimal total
    ) {}

    public record ActividadRecienteDTO(
            Long reservaId,
            String clienteNombreCompleto,
            String habitacionNumero,
            String tipoHabitacion,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            String estado,
            BigDecimal montoTotal,
            String fechaCreacion
    ) {}

    public record PrediccionOcupacionDTO(
            String semana,
            String rangoFechas,
            double ocupacionProyectada,
            int habitacionesEstimadas,
            String factorTendencia,
            String nivelDemanda,
            String recomendacionTarifa
    ) {}
}
