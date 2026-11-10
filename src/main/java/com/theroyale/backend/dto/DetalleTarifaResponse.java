package com.theroyale.backend.dto;

import java.math.BigDecimal;

public record DetalleTarifaResponse(
        BigDecimal precioBase,
        double factorOcupacion,
        double factorTemporada,
        double factorFinDeSemana,
        double multiplicadorTotal,
        BigDecimal precioFinalNoche,
        BigDecimal precioTotalEstadia,
        long noches,
        String etiquetaDemanda
) {}
