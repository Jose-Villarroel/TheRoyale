package com.theroyale.backend.dto;

import com.theroyale.backend.model.Cliente;
import com.theroyale.backend.model.EstadoReserva;
import com.theroyale.backend.model.Habitacion;
import com.theroyale.backend.model.Reserva;
import com.theroyale.backend.model.TipoHabitacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// ===== Reserva tal como la consumen el portal del huésped y el del operador =====
// Aplana cliente, habitación y tipo de habitación en campos sueltos: el frontend tiene todo lo
// que muestra (nombre del huésped, número de habitación, imagen del tipo) sin objetos anidados.
public record ReservaResponse(
        Long id,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        EstadoReserva estado,
        Integer cantidadPersonas,
        BigDecimal precioNocheAcordado,
        LocalDateTime fechaCreacion,
        Long clienteId,
        String clienteNombre,
        String clienteApellido,
        String clienteEmail,
        String clienteTelefono,
        String clienteCedula,
        Long habitacionId,
        String habitacionNumero,
        Long tipoHabitacionId,
        String tipoHabitacionNombre,
        String tipoHabitacionImagenUrl
) {

    public static ReservaResponse from(Reserva r) {
        Cliente cliente = r.getCliente();
        Habitacion habitacion = r.getHabitacion();
        TipoHabitacion tipo = habitacion == null ? null : habitacion.getTipoHabitacion();

        return new ReservaResponse(
                r.getId(),
                r.getFechaInicio(),
                r.getFechaFin(),
                r.getEstado(),
                r.getCantidadPersonas(),
                r.getPrecioNocheAcordado(),
                r.getFechaCreacion(),
                cliente == null ? null : cliente.getId(),
                cliente == null ? null : cliente.getNombre(),
                cliente == null ? null : cliente.getApellido(),
                cliente == null ? null : cliente.getEmail(),
                cliente == null ? null : cliente.getTelefono(),
                cliente == null ? null : cliente.getCedula(),
                habitacion == null ? null : habitacion.getId(),
                habitacion == null ? null : habitacion.getNumero(),
                tipo == null ? null : tipo.getId(),
                tipo == null ? null : tipo.getNombre(),
                tipo == null ? null : tipo.getImagenUrl());
    }
}
