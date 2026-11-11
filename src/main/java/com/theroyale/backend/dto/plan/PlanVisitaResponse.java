package com.theroyale.backend.dto.plan;

import com.theroyale.backend.model.EstadoReserva;
import com.theroyale.backend.model.FuentePlan;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// ===== Plan de visita tal como lo consume la página "my-reservations/:id/plan" del frontend =====
// Trae los datos de la reserva para el encabezado ("Dec 12–15 · Luxury · 2 guests") sin datos
// personales del huésped (nombre, email, cédula, teléfono).
public record PlanVisitaResponse(
        Long reservaId,
        EstadoReserva estadoReserva,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        long noches,
        String tipoHabitacionNombre,
        Integer cantidadPersonas,
        String resumen,
        String descripcion,
        List<EntendidoItem> entendido,
        PreferenciasPlan preferencias,
        boolean requiereRevision,
        List<LocalDate> diasEnRevision,
        List<FuentePlan> fuentesUsadas,
        List<FuentePlan> fuentesNoDisponibles,
        int generacionesRestantesHoy,
        LocalDateTime fechaGeneracion,
        LocalDateTime fechaActualizacion,
        List<DiaPlanResponse> dias
) {
}
