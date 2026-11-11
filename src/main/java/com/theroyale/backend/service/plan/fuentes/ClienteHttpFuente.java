package com.theroyale.backend.service.plan.fuentes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

// ===== GET de solo lectura hacia una API pública, con timeout y errores normalizados =====
// Usa RestClient de Spring 6.1, igual que GeminiRestClient (sin dependencias nuevas). Cualquier
// fallo (HTTP no exitoso, corte, JSON ilegible) se convierte en FuenteNoDisponibleException.
public class ClienteHttpFuente {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String nombreFuente;

    public ClienteHttpFuente(String nombreFuente, int timeoutSegundos, ObjectMapper objectMapper) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(timeoutSegundos * 1000);
        fabrica.setReadTimeout(timeoutSegundos * 1000);
        this.restClient = RestClient.builder().requestFactory(fabrica).build();
        this.objectMapper = objectMapper;
        this.nombreFuente = nombreFuente;
    }

    public JsonNode obtenerJson(URI uri, Map<String, String> headers) {
        String cuerpo;
        try {
            cuerpo = restClient.get()
                    .uri(uri)
                    .headers(h -> headers.forEach(h::add))
                    .exchange((peticion, respuesta) -> {
                        String texto = new String(respuesta.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        if (respuesta.getStatusCode().isError()) {
                            throw new FuenteNoDisponibleException(nombreFuente + " respondió "
                                    + respuesta.getStatusCode().value() + ": " + recortar(texto));
                        }
                        return texto;
                    });
        } catch (RestClientException ex) {
            throw new FuenteNoDisponibleException("No se pudo consultar " + nombreFuente + ": " + ocultarClaves(ex.getMessage()));
        }
        try {
            return objectMapper.readTree(cuerpo);
        } catch (IOException ex) {
            throw new FuenteNoDisponibleException(nombreFuente + " devolvió un JSON ilegible.", ex);
        }
    }

    // El mensaje de RestClient incluye la URL: la clave de Ticketmaster (apikey=...) no debe llegar al log.
    // Por lo mismo no se encadena la excepción original como causa.
    static String ocultarClaves(String texto) {
        return texto == null ? null : texto.replaceAll("(?i)(apikey=)[^&\\s\"]+", "$1***");
    }

    private static String recortar(String texto) {
        return texto.length() > 300 ? texto.substring(0, 300) + "..." : texto;
    }
}
