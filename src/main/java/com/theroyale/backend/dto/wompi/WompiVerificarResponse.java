package com.theroyale.backend.dto.wompi;

import com.theroyale.backend.dto.ReservaResponse;

public record WompiVerificarResponse(
        boolean exitoso,
        String status,
        String mensaje,
        String transactionId,
        String metodoPago,
        ReservaResponse reserva
) {}
