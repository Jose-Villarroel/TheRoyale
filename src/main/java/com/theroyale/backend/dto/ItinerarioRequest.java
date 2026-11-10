package com.theroyale.backend.dto;

public record ItinerarioRequest(
        int dias,
        String estilo,
        String acompanantes,
        String preferencias
) {}
