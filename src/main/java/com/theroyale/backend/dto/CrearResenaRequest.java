package com.theroyale.backend.dto;

// ===== Datos que envía el huésped para crear o editar una reseña =====
// POST usa los cinco campos. PUT usa calificacion, titulo y comentario, más clienteId para validar
// la autoría (reservaId se ignora: editar no cambia la reserva ni el tipo, RN-7).
// Mientras no haya JWT, clienteId viaja en el cuerpo, igual que en CrearReservaRequest.
public record CrearResenaRequest(
        Long reservaId,
        Long clienteId,
        Integer calificacion,
        String titulo,
        String comentario
) {
}
