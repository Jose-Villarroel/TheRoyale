package com.theroyale.backend.service.plan;

import com.theroyale.backend.model.CategoriaLugar;
import com.theroyale.backend.model.FuentePlan;
import com.theroyale.backend.model.RestauranteHotel;
import com.theroyale.backend.service.plan.fuentes.ClimaDia;
import com.theroyale.backend.service.plan.fuentes.EventoCandidato;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// ===== Todo lo que se le manda a Gemini en una generación, con sus identificadores =====
// El validador revisa cada bloque contra ESTE contexto (el de la misma generación), no contra la base.
// No incluye ningún dato personal del huésped.
public record ContextoPlan(
        List<LocalDate> dias,
        Map<LocalDate, ClimaDia> clima,
        List<LugarContexto> lugares,
        List<EventoContexto> eventos,
        List<ServicioContexto> servicios,
        List<RestauranteContexto> restaurantes,
        List<FuentePlan> fuentesUsadas,
        List<FuentePlan> fuentesNoDisponibles
) {

    public record LugarContexto(Long id, String nombre, CategoriaLugar categoria, boolean bajoTecho,
                                int duracionMinutos, String horario, String url, double distanciaMetros) {
    }

    // distanciaMetros es null si el evento no trae coordenadas ni se pudieron estimar
    public record EventoContexto(EventoCandidato evento, Double distanciaMetros) {
    }

    public record ServicioContexto(Long id, String nombre, double precio, List<String> caracteristicas) {
    }

    // opcionesDieta: p. ej. "VEGANO: 4 dishes" (solo si el huésped indicó una dieta)
    public record RestauranteContexto(RestauranteHotel restaurante, String nombre, String horario,
                                      List<String> opcionesDieta) {
    }

    public Optional<LugarContexto> lugar(String id) {
        return lugares.stream().filter(l -> String.valueOf(l.id()).equals(id)).findFirst();
    }

    public Optional<EventoContexto> evento(String id) {
        return eventos.stream().filter(e -> e.evento().id().equals(id)).findFirst();
    }

    public Optional<ServicioContexto> servicio(String id) {
        return servicios.stream().filter(s -> String.valueOf(s.id()).equals(id)).findFirst();
    }

    public Optional<RestauranteContexto> restaurante(String id) {
        return restaurantes.stream().filter(r -> r.restaurante().name().equals(id)).findFirst();
    }
}
