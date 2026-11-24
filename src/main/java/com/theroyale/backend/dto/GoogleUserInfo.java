package com.theroyale.backend.dto;

public record GoogleUserInfo(
        String sub,
        String email,
        String name,
        String givenName,
        String familyName,
        String picture
) {}
