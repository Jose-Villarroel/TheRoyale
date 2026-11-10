package com.theroyale.backend.dto;

import java.math.BigDecimal;

public record ConfirmarStripeRequest(
        Long reservaId,
        String paymentIntentId,
        BigDecimal monto
) {}
