package com.theroyale.backend.service;

import com.theroyale.backend.dto.DashboardResponse;
import com.theroyale.backend.model.*;
import com.theroyale.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

// ===== Servicio de Analítica y Métricas para el Dashboard de Administración =====
@Service
@Transactional(readOnly = true)
public class DashboardService {

    @Autowired
    private HabitacionRepository habitacionRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private TipoHabitacionRepository tipoHabitacionRepository;

    public DashboardResponse obtenerMetricas() {
        LocalDate hoy = LocalDate.now();

        // 1. Inventario y ocupación de habitaciones
        List<Habitacion> habitaciones = habitacionRepository.findAll();
        long totalHabitaciones = habitaciones.size();
        long habitacionesOcupadas = habitaciones.stream()
                .filter(h -> h.getEstado() == EstadoHabitacion.OCUPADA)
                .count();
        long habitacionesDisponibles = habitaciones.stream()
                .filter(h -> h.getEstado() == EstadoHabitacion.DISPONIBLE)
                .count();
        long habitacionesMantenimiento = habitaciones.stream()
                .filter(h -> h.getEstado() == EstadoHabitacion.MANTENIMIENTO || h.getEstado() == EstadoHabitacion.DESHABILITADA)
                .count();

        double porcentajeOcupacion = totalHabitaciones > 0
                ? BigDecimal.valueOf((habitacionesOcupadas * 100.0) / totalHabitaciones)
                    .setScale(1, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        Map<String, Long> habitacionesPorEstado = habitaciones.stream()
                .collect(Collectors.groupingBy(h -> h.getEstado().name(), Collectors.counting()));

        // 2. Flujo del día (Check-ins y Check-outs)
        long llegadasHoy = reservaRepository.findByFechaInicioAndEstadoIn(hoy,
                List.of(EstadoReserva.CONFIRMADA, EstadoReserva.PENDIENTE)).size();
        long salidasHoy = reservaRepository.findByFechaFinAndEstado(hoy, EstadoReserva.EN_CURSO).size();

        long reservasActivas = reservaRepository.countByEstado(EstadoReserva.CONFIRMADA)
                + reservaRepository.countByEstado(EstadoReserva.EN_CURSO);
        long totalClientes = clienteRepository.count();

        // 3. Métricas Financieras (Ingresos)
        List<Pago> pagos = pagoRepository.findAll();
        BigDecimal ingresosTotales = pagos.stream()
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDateTime inicioMes = hoy.withDayOfMonth(1).atStartOfDay();
        BigDecimal ingresosMesActual = pagos.stream()
                .filter(p -> !p.getFecha().isBefore(inicioMes))
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4. Distribución por tipo de habitación
        List<TipoHabitacion> tipos = tipoHabitacionRepository.findAll();
        List<DashboardResponse.OcupacionTipoDTO> ocupacionPorTipo = tipos.stream().map(tipo -> {
            List<Habitacion> habsDeTipo = habitaciones.stream()
                    .filter(h -> h.getTipoHabitacion().getId().equals(tipo.getId()))
                    .toList();
            long total = habsDeTipo.size();
            long ocupadas = habsDeTipo.stream()
                    .filter(h -> h.getEstado() == EstadoHabitacion.OCUPADA)
                    .count();
            double pct = total > 0
                    ? BigDecimal.valueOf((ocupadas * 100.0) / total).setScale(1, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;
            return new DashboardResponse.OcupacionTipoDTO(tipo.getId(), tipo.getNombre(), total, ocupadas, pct);
        }).toList();

        // 5. Histórico de ingresos mensuales (últimos 6 meses)
        List<DashboardResponse.IngresoMensualDTO> ingresosMensuales = generarHistoricoMensual(pagos, hoy);

        // 6. Actividades / Reservas recientes
        List<DashboardResponse.ActividadRecienteDTO> actividadReciente = reservaRepository.findAllByOrderByFechaInicioDesc()
                .stream()
                .limit(7)
                .map(r -> {
                    Cliente c = r.getCliente();
                    Habitacion h = r.getHabitacion();
                    TipoHabitacion t = h != null ? h.getTipoHabitacion() : null;
                    BigDecimal totalEstimado = r.getPrecioNocheAcordado().multiply(BigDecimal.valueOf(r.getNoches()));
                    return new DashboardResponse.ActividadRecienteDTO(
                            r.getId(),
                            c != null ? c.getNombre() + " " + c.getApellido() : "Guest",
                            h != null ? h.getNumero() : "N/A",
                            t != null ? t.getNombre() : "N/A",
                            r.getFechaInicio(),
                            r.getFechaFin(),
                            r.getEstado().name(),
                            totalEstimado,
                            r.getFechaCreacion() != null
                                    ? r.getFechaCreacion().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                                    : "Recent"
                    );
                })
                .toList();

        return new DashboardResponse(
                porcentajeOcupacion,
                habitacionesOcupadas,
                habitacionesDisponibles,
                habitacionesMantenimiento,
                totalHabitaciones,
                llegadasHoy,
                salidasHoy,
                reservasActivas,
                totalClientes,
                ingresosMesActual,
                ingresosTotales,
                ocupacionPorTipo,
                ingresosMensuales,
                habitacionesPorEstado,
                actividadReciente
        );
    }

    private List<DashboardResponse.IngresoMensualDTO> generarHistoricoMensual(List<Pago> pagos, LocalDate hoy) {
        List<DashboardResponse.IngresoMensualDTO> list = new ArrayList<>();
        DateTimeFormatter mesFormato = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);

        for (int i = 5; i >= 0; i--) {
            YearMonth ym = YearMonth.from(hoy.minusMonths(i));
            String mesNombre = ym.format(mesFormato);
            String anioMes = ym.toString();

            // Filtrar pagos que caigan en este mes
            BigDecimal totalPagos = pagos.stream()
                    .filter(p -> YearMonth.from(p.getFecha()).equals(ym))
                    .map(Pago::getMonto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Desglose proporcional de demostración para gráficas (70% hospedaje, 30% servicios)
            BigDecimal hospedaje = totalPagos.multiply(BigDecimal.valueOf(0.70)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal servicios = totalPagos.subtract(hospedaje);

            list.add(new DashboardResponse.IngresoMensualDTO(mesNombre, anioMes, hospedaje, servicios, totalPagos));
        }

        return list;
    }
}
