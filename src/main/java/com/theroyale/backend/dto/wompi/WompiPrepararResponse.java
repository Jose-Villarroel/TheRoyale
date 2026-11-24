package com.theroyale.backend.dto.wompi;

public record WompiPrepararResponse(
        Long reservaId,
        String reference,
        long amountInCents,
        double amountInUsd,
        double amountInCop,
        String currency,
        String signature,
        String publicKey,
        String customerEmail,
        String customerName
) {}
