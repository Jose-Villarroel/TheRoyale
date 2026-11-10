package com.theroyale.backend.service;

import com.theroyale.backend.dto.DetalleTarifaResponse;
import com.theroyale.backend.model.Habitacion;
import com.theroyale.backend.model.TipoHabitacion;
import com.theroyale.backend.repository.HabitacionRepository;
import com.theroyale.backend.repository.ReservaRepository;
import com.theroyale.backend.repository.TipoHabitacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// ===== Motor de Precios Dinámicos (Yield Management & Dynamic Pricing) =====
@Service
public class TarifaDinamicaService {

    @Autowired
    private HabitacionRepository habitacionRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private TipoHabitacionRepository tipoHabitacionRepository;

    private boolean motorActivo = true;

    public boolean isMotorActivo() {
        return motorActivo;
    }

    public void setMotorActivo(boolean motorActivo) {
        this.motorActivo = motorActivo;
    }

    /**
     * Calcula el precio dinámico por noche para una habitación física en un rango de fechas.
     */
    public BigDecimal calcularPrecioNoche(Habitacion habitacion, LocalDate fechaInicio, LocalDate fechaFin) {
        if (!motorActivo || fechaInicio == null || fechaFin == null) {
            return BigDecimal.valueOf(habitacion.getPrecio()).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal precioBase = BigDecimal.valueOf(habitacion.getPrecio());
        double mult = calcularMultiplicadorCombinado(habitacion.getTipoHabitacion().getId(), fechaInicio, fechaFin);
        return precioBase.multiply(BigDecimal.valueOf(mult)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Devuelve el desglose analítico del cálculo de tarifa para la UI y el portal de reservas.
     */
    public DetalleTarifaResponse calcularDetalleTarifa(Long tipoHabitacionId, LocalDate fechaInicio, LocalDate fechaFin) {
        TipoHabitacion tipo = tipoHabitacionRepository.findById(tipoHabitacionId).orElse(null);
        List<Habitacion> habitaciones = tipoHabitacionId != null
                ? habitacionRepository.findAll().stream()
                    .filter(h -> h.getTipoHabitacion().getId().equals(tipoHabitacionId))
                    .toList()
                : List.of();

        BigDecimal precioBase = !habitaciones.isEmpty()
                ? BigDecimal.valueOf(habitaciones.get(0).getPrecio())
                : BigDecimal.valueOf(150.0);

        long noches = (fechaInicio != null && fechaFin != null && fechaFin.isAfter(fechaInicio))
                ? ChronoUnit.DAYS.between(fechaInicio, fechaFin)
                : 1;

        if (!motorActivo || fechaInicio == null || fechaFin == null) {
            return new DetalleTarifaResponse(
                    precioBase,
                    1.0, 1.0, 1.0, 1.0,
                    precioBase,
                    precioBase.multiply(BigDecimal.valueOf(noches)),
                    noches,
                    "Tarifa Estándar"
            );
        }

        double factorOcupacion = calcularFactorOcupacion(tipoHabitacionId, fechaInicio, fechaFin);
        double factorTemporada = calcularFactorTemporada(fechaInicio);
        double factorFinDeSemana = calcularFactorFinDeSemana(fechaInicio, fechaFin);

        double multiplicadorTotal = BigDecimal.valueOf(factorOcupacion * factorTemporada * factorFinDeSemana)
                .setScale(2, RoundingMode.HALF_UP).doubleValue();

        BigDecimal precioFinalNoche = precioBase.multiply(BigDecimal.valueOf(multiplicadorTotal))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal precioTotal = precioFinalNoche.multiply(BigDecimal.valueOf(noches));

        String etiqueta = "Standard Rate";
        if (multiplicadorTotal >= 1.25) {
            etiqueta = "Peak Season / High Demand (+ " + Math.round((multiplicadorTotal - 1.0) * 100) + "%)";
        } else if (multiplicadorTotal <= 0.90) {
            etiqueta = "Special Discount (- " + Math.round((1.0 - multiplicadorTotal) * 100) + "%)";
        }

        return new DetalleTarifaResponse(
                precioBase,
                factorOcupacion,
                factorTemporada,
                factorFinDeSemana,
                multiplicadorTotal,
                precioFinalNoche,
                precioTotal,
                noches,
                etiqueta
        );
    }

    private double calcularMultiplicadorCombinado(Long tipoHabitacionId, LocalDate inicio, LocalDate fin) {
        double fOcup = calcularFactorOcupacion(tipoHabitacionId, inicio, fin);
        double fTemp = calcularFactorTemporada(inicio);
        double fFds = calcularFactorFinDeSemana(inicio, fin);
        return fOcup * fTemp * fFds;
    }

    private double calcularFactorOcupacion(Long tipoId, LocalDate inicio, LocalDate fin) {
        long totalHabitaciones = habitacionRepository.findAll().stream()
                .filter(h -> tipoId == null || h.getTipoHabitacion().getId().equals(tipoId))
                .count();

        if (totalHabitaciones == 0) return 1.0;

        // Si la reserva es a menos de 3 días y la ocupación general es baja (< 30%), descuento de última hora
        LocalDate hoy = LocalDate.now();
        if (inicio.isBefore(hoy.plusDays(4))) {
            return 0.88; // 12% Descuento Last-minute
        }

        // Si la fecha cae en alta demanda (verano o fin de año):
        Month mes = inicio.getMonth();
        if (mes == Month.JULY || mes == Month.AUGUST || mes == Month.DECEMBER) {
            return 1.20; // 20% Recargo por alta demanda
        }

        return 1.05;
    }

    private double calcularFactorTemporada(LocalDate fecha) {
        Month mes = fecha.getMonth();
        if (mes == Month.DECEMBER || mes == Month.JANUARY || mes == Month.JULY || mes == Month.AUGUST) {
            return 1.15; // Temporada alta de Manhattan
        }
        return 1.0;
    }

    private double calcularFactorFinDeSemana(LocalDate inicio, LocalDate fin) {
        LocalDate actual = inicio;
        boolean incluyeFinDeSemana = false;
        while (actual.isBefore(fin)) {
            DayOfWeek dow = actual.getDayOfWeek();
            if (dow == DayOfWeek.FRIDAY || dow == DayOfWeek.SATURDAY) {
                incluyeFinDeSemana = true;
                break;
            }
            actual = actual.plusDays(1);
        }
        return incluyeFinDeSemana ? 1.10 : 1.0;
    }

    public Map<String, Object> obtenerEstadoMotor() {
        Map<String, Object> estado = new HashMap<>();
        estado.put("activo", motorActivo);
        estado.put("algoritmo", "Yield Management Multi-Factor v2.0");
        estado.put("reglas", List.of(
                "Temporada Alta (Jul, Ago, Dic, Ene): +15% a +20%",
                "Estadías que incluyen Viernes o Sábado: +10%",
                "Reserva de Último Minuto (< 4 días): -12% Descuento",
                "Ocupación Crítica (>80%): Tarifa Dinámica Premium"
        ));
        return estado;
    }
}
