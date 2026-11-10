package com.theroyale.backend.dto;

public record GoogleAuthRequest(
        String email,
        String nombre,
        String apellido,
        String googleIdToken
) {}
