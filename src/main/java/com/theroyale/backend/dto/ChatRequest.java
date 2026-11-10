package com.theroyale.backend.dto;

public record ChatRequest(
        String mensaje,
        String conversacionId
) {}
