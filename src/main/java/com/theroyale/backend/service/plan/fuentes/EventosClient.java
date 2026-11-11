package com.theroyale.backend.service.plan.fuentes;

import com.theroyale.backend.model.FuentePlan;

import java.time.LocalDate;
import java.util.List;

// ===== Puerto hacia una fuente de eventos de la ciudad (NYC Open Data o Ticketmaster) =====
// Devuelve los eventos que se cruzan con el rango de fechas, ya normalizados; ContextoPlanService
// aplica después los filtros de distancia y cantidad. Si falla lanza una RuntimeException.
public interface EventosClient {

    FuentePlan fuente();

    List<EventoCandidato> buscar(LocalDate inicio, LocalDate fin);
}
