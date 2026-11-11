package com.theroyale.backend.service.plan.fuentes;

import com.theroyale.backend.model.FuentePlan;

import java.time.LocalDate;
import java.time.LocalDateTime;

// ===== Evento de la ciudad en el formato interno común a NYC Open Data y Ticketmaster =====
// id lleva el prefijo de la fuente ("nyc-…", "tm-…") para que no choquen entre fuentes.
// latitud/longitud pueden faltar (NYC Open Data no trae coordenadas y no siempre se pueden estimar).
public record EventoCandidato(
        String id,
        FuentePlan fuente,
        String titulo,
        LocalDateTime inicio,
        LocalDateTime fin,
        String lugar,
        Double latitud,
        Double longitud,
        String categoria,
        String url,
        boolean bajoTecho
) {

    // true si el evento ocurre (al menos en parte) ese día
    public boolean ocurreEl(LocalDate dia) {
        return !inicio.toLocalDate().isAfter(dia) && !fin.toLocalDate().isBefore(dia);
    }

    public boolean tieneCoordenadas() {
        return latitud != null && longitud != null;
    }
}
