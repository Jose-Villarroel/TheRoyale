package com.theroyale.backend.service.plan.fuentes;

import com.theroyale.backend.model.TipoClima;

import java.time.LocalDate;

// ===== Clima de un día en el formato interno común a NWS y Open-Meteo (°C y % de lluvia) =====
public record ClimaDia(
        LocalDate fecha,
        String resumen,
        int maxC,
        int minC,
        Integer probLluvia,
        TipoClima tipo
) {

    public int probLluviaOCero() {
        return probLluvia == null ? 0 : probLluvia;
    }
}
