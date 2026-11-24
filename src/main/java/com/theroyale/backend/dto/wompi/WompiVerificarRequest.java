package com.theroyale.backend.dto.wompi;

public record WompiVerificarRequest(
        String transactionId,
        Long reservaId
) {}
