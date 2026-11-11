package com.theroyale.backend.service.plan.fuentes;

import com.theroyale.backend.model.FuentePlan;

import java.time.LocalDate;
import java.util.List;

// ===== Puerto hacia una fuente de clima (NWS u Open-Meteo) =====
// Es una interfaz para que las pruebas y el perfil sin fuentes usen datos fijos: nunca se sale a internet.
// Si la fuente falla lanza FuenteNoDisponibleException (u otra RuntimeException) y el plan sigue sin ella.
public interface ClimaClient {

    FuentePlan fuente();

    // Pronóstico de los días del rango que la fuente cubre (NWS: próximos 7 días; Open-Meteo: 16 días).
    // Los días que no cubre simplemente no vienen en la lista.
    List<ClimaDia> pronostico(LocalDate inicio, LocalDate fin);

    // Clima típico de esas fechas (solo Open-Meteo: promedio de los mismos días de los 3 años anteriores)
    default List<ClimaDia> climaTipico(LocalDate inicio, LocalDate fin) {
        return List.of();
    }
}
