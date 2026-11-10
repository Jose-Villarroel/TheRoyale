package com.theroyale.backend.dto;

import java.math.BigDecimal;

public record StripeIntentResponse(
        String clientSecret,
        String paymentIntentId,
        BigDecimal monto,
        String moneda,
        String status,
        String publishableKey
) {}
