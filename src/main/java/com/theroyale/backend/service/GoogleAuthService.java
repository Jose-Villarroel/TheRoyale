package com.theroyale.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theroyale.backend.dto.GoogleUserInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Service
public class GoogleAuthService {

    @Value("${google.client.id}")
    private String configuredClientId;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Optional<GoogleUserInfo> verificarToken(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            return Optional.empty();
        }

        try {
            String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken.trim();
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return Optional.empty();
            }

            JsonNode node = objectMapper.readTree(response.getBody());

            // 1. Validar que la audiencia (aud) coincida con nuestro Client ID
            String aud = node.has("aud") ? node.get("aud").asText() : "";
            if (!configuredClientId.equals(aud)) {
                return Optional.empty();
            }

            // 2. Validar que el emisor (iss) sea Google
            String iss = node.has("iss") ? node.get("iss").asText() : "";
            if (!iss.equals("accounts.google.com") && !iss.equals("https://accounts.google.com")) {
                return Optional.empty();
            }

            // 3. Extraer datos del usuario
            String sub = node.has("sub") ? node.get("sub").asText() : "";
            String email = node.has("email") ? node.get("email").asText() : "";
            String name = node.has("name") ? node.get("name").asText() : "";
            String givenName = node.has("given_name") ? node.get("given_name").asText() : name;
            String familyName = node.has("family_name") ? node.get("family_name").asText() : "";
            String picture = node.has("picture") ? node.get("picture").asText() : "";

            if (email.isBlank()) {
                return Optional.empty();
            }

            return Optional.of(new GoogleUserInfo(sub, email, name, givenName, familyName, picture));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
