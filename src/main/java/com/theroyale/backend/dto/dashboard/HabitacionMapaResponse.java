package com.theroyale.backend.dto.dashboard;

// ===== Una habitación en el mapa por pisos =====
// piso se deriva del número de habitación (la 101 es piso 1, la 901 piso 9) porque el modelo no
// guarda el piso: es la convención de numeración del hotel que siembra el DataLoader.
public record HabitacionMapaResponse(
        Long id,
        String numero,
        String piso,
        String estado,
        String tipoNombre
) {
}
