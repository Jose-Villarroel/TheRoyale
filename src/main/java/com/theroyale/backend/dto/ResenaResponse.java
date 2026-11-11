package com.theroyale.backend.dto;

import com.theroyale.backend.model.Cliente;
import com.theroyale.backend.model.Resena;
import com.theroyale.backend.model.Reserva;
import com.theroyale.backend.model.TipoHabitacion;

import java.time.LocalDate;
import java.time.LocalDateTime;

// ===== Reseña tal como la consumen la página del tipo, "My reservations" y la moderación =====
// Aplanada como ReservaResponse. Privacidad: del autor solo se expone su id y "Nombre I."
// (nombre + inicial del apellido); nunca el email, la cédula ni el teléfono.
public record ResenaResponse(
        Long id,
        Integer calificacion,
        String titulo,
        String comentario,
        boolean visible,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaModificacion,
        Long reservaId,
        LocalDate fechaInicioEstadia,
        LocalDate fechaFinEstadia,
        Long clienteId,
        String clienteNombre,
        Long tipoHabitacionId,
        String tipoHabitacionNombre
) {

    public static ResenaResponse from(Resena r) {
        Reserva reserva = r.getReserva();
        Cliente cliente = r.getCliente();
        TipoHabitacion tipo = r.getTipoHabitacion();

        return new ResenaResponse(
                r.getId(),
                r.getCalificacion(),
                r.getTitulo(),
                r.getComentario(),
                r.isVisible(),
                r.getFechaCreacion(),
                r.getFechaModificacion(),
                reserva == null ? null : reserva.getId(),
                reserva == null ? null : reserva.getFechaInicio(),
                reserva == null ? null : reserva.getFechaFin(),
                cliente == null ? null : cliente.getId(),
                nombrePublico(cliente),
                tipo == null ? null : tipo.getId(),
                tipo == null ? null : tipo.getNombre());
    }

    // "Laura Martínez" -> "Laura M."; sin apellido queda solo el nombre
    static String nombrePublico(Cliente cliente) {
        if (cliente == null) {
            return null;
        }
        String nombre = cliente.getNombre() == null ? "" : cliente.getNombre().trim();
        String apellido = cliente.getApellido() == null ? "" : cliente.getApellido().trim();
        if (apellido.isEmpty()) {
            return nombre;
        }
        String inicial = new String(Character.toChars(apellido.codePointAt(0))).toUpperCase();
        return (nombre + " " + inicial + ".").trim();
    }
}
