package com.theroyale.backend.service.plan;

import com.theroyale.backend.dto.plan.DiaPlanResponse;
import com.theroyale.backend.model.Reserva;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

// ===== Lo único de la reserva que se usa para planificar (y lo único que ve Gemini de ella) =====
// Fechas, número de huéspedes y tipo de habitación: nunca nombre, email, cédula ni teléfono.
public record DatosEstadia(
        LocalDate fechaInicio,
        LocalDate fechaFin,
        int cantidadPersonas,
        String tipoHabitacion
) {

    public static final LocalTime CHECK_IN = LocalTime.of(15, 0);
    public static final LocalTime CHECK_OUT = LocalTime.of(12, 0);

    public static DatosEstadia desde(Reserva reserva) {
        String tipo = reserva.getHabitacion() == null || reserva.getHabitacion().getTipoHabitacion() == null
                ? null : reserva.getHabitacion().getTipoHabitacion().getNombre();
        return new DatosEstadia(reserva.getFechaInicio(), reserva.getFechaFin(),
                reserva.getCantidadPersonas() == null ? 1 : reserva.getCantidadPersonas(), tipo);
    }

    public long noches() {
        return ChronoUnit.DAYS.between(fechaInicio, fechaFin);
    }

    public String tipoDia(LocalDate fecha) {
        if (fecha.equals(fechaInicio)) {
            return DiaPlanResponse.LLEGADA;
        }
        return fecha.equals(fechaFin) ? DiaPlanResponse.SALIDA : DiaPlanResponse.COMPLETO;
    }

    public int numero(LocalDate fecha) {
        return (int) ChronoUnit.DAYS.between(fechaInicio, fecha) + 1;
    }
}
