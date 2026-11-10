package com.theroyale.backend.dto;

import java.math.BigDecimal;

public record StripeIntentRequest(
        Long reservaId,
        BigDecimal monto,
        String moneda
) {}
