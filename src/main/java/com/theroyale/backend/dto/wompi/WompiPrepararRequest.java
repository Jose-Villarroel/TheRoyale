package com.theroyale.backend.dto.wompi;

import java.time.LocalDate;

public record WompiPrepararRequest(
        Long clienteId,
        Long tipoHabitacionId,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        Integer cantidadPersonas
) {}
