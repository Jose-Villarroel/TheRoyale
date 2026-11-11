package com.theroyale.backend.dto.plan;

import java.time.LocalDate;
import java.util.List;

// ===== Un día del plan: tipo (LLEGADA, COMPLETO, SALIDA), clima y bloques ordenados por hora =====
public record DiaPlanResponse(
        LocalDate fecha,
        int numero,
        String tipoDia,
        String titulo,
        ClimaDiaResponse clima,
        List<BloquePlanResponse> bloques
) {

    public static final String LLEGADA = "LLEGADA";
    public static final String COMPLETO = "COMPLETO";
    public static final String SALIDA = "SALIDA";

    public DiaPlanResponse conClima(ClimaDiaResponse nuevoClima) {
        return new DiaPlanResponse(fecha, numero, tipoDia, titulo, nuevoClima, bloques);
    }

    public DiaPlanResponse conBloques(List<BloquePlanResponse> nuevosBloques) {
        return new DiaPlanResponse(fecha, numero, tipoDia, titulo, clima, nuevosBloques);
    }
}
