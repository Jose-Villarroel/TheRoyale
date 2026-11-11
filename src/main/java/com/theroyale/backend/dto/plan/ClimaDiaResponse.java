package com.theroyale.backend.dto.plan;

import com.theroyale.backend.model.TipoClima;

// ===== Clima de un día del plan; tipo dice si es pronóstico (NWS / Open-Meteo) o clima típico =====
public record ClimaDiaResponse(
        String resumen,
        Integer maxC,
        Integer minC,
        Integer probLluvia,
        TipoClima tipo
) {
}
