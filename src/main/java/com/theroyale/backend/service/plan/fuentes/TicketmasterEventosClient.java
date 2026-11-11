package com.theroyale.backend.service.plan.fuentes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theroyale.backend.config.HotelProperties;
import com.theroyale.backend.config.PlanVisitaProperties;
import com.theroyale.backend.model.FuentePlan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// ===== Conciertos, teatro, deportes y familia cerca del hotel (Ticketmaster Discovery API v2) =====
// Requiere TICKETMASTER_API_KEY (gratis en developer.ticketmaster.com); sin clave la fuente queda
// apagada y aparece en fuentesNoDisponibles. Respuesta documentada: _embedded.events[] con id, name,
// url, dates.start (localDate, localTime), classifications[].segment.name y _embedded.venues[]
// (name, location.latitude / longitude como texto). Caché de 6 h.
@Component
@ConditionalOnProperty(prefix = "plan.fuentes", name = "habilitadas", havingValue = "true", matchIfMissing = true)
public class TicketmasterEventosClient implements EventosClient {

    private static final String URL_EVENTOS = "https://app.ticketmaster.com/discovery/v2/events.json";
    private static final ZoneId ZONA_NUEVA_YORK = ZoneId.of("America/New_York");
    private static final DateTimeFormatter FORMATO_UTC = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    // Ticketmaster no siempre informa la hora de fin: se asume una función de 3 horas
    static final Duration DURACION_SUPUESTA = Duration.ofHours(3);
    private static final LocalTime HORA_SUPUESTA = LocalTime.of(19, 0);

    private final ClienteHttpFuente http;
    private final HotelProperties hotel;
    private final PlanVisitaProperties propiedades;
    private final String urlEventos;
    private final CacheTtl cache = new CacheTtl(Duration.ofHours(6));

    @Autowired
    public TicketmasterEventosClient(HotelProperties hotel, PlanVisitaProperties propiedades, ObjectMapper objectMapper) {
        this(hotel, propiedades, objectMapper, URL_EVENTOS);
    }

    // Para las pruebas: apunta a un servidor local que imita a Ticketmaster
    TicketmasterEventosClient(HotelProperties hotel, PlanVisitaProperties propiedades, ObjectMapper objectMapper,
                              String urlEventos) {
        this.hotel = hotel;
        this.propiedades = propiedades;
        this.urlEventos = urlEventos;
        this.http = new ClienteHttpFuente("Ticketmaster", propiedades.getTimeoutFuenteSegundos(), objectMapper);
    }

    @Override
    public FuentePlan fuente() {
        return FuentePlan.TICKETMASTER;
    }

    @Override
    public List<EventoCandidato> buscar(LocalDate inicio, LocalDate fin) {
        if (!propiedades.getTicketmaster().tieneClave()) {
            throw new FuenteNoDisponibleException("Ticketmaster está apagado: falta TICKETMASTER_API_KEY.");
        }
        return cache.obtener(inicio + ":" + fin, () -> parsear(http.obtenerJson(construirUri(inicio, fin), Map.of())));
    }

    URI construirUri(LocalDate inicio, LocalDate fin) {
        String desde = inicio.atStartOfDay(ZONA_NUEVA_YORK).withZoneSameInstant(ZoneOffset.UTC).format(FORMATO_UTC);
        String hasta = fin.atTime(23, 59, 59).atZone(ZONA_NUEVA_YORK).withZoneSameInstant(ZoneOffset.UTC).format(FORMATO_UTC);
        return UriComponentsBuilder.fromHttpUrl(urlEventos)
                .queryParam("apikey", propiedades.getTicketmaster().getApiKey())
                .queryParam("latlong", String.format(Locale.US, "%.4f,%.4f", hotel.getLatitud(), hotel.getLongitud()))
                .queryParam("radius", 5)
                .queryParam("unit", "miles")
                .queryParam("startDateTime", desde)
                .queryParam("endDateTime", hasta)
                .queryParam("size", 50)
                .queryParam("sort", "date,asc")
                .encode()
                .build()
                .toUri();
    }

    static List<EventoCandidato> parsear(JsonNode respuesta) {
        if (respuesta.has("fault") || respuesta.has("errors")) {
            throw new FuenteNoDisponibleException("Ticketmaster respondió con error.");
        }
        List<EventoCandidato> eventos = new ArrayList<>();
        // Sin resultados Ticketmaster responde sin "_embedded" (solo "page"): no es un error
        for (JsonNode evento : respuesta.path("_embedded").path("events")) {
            JsonNode inicio = evento.path("dates").path("start");
            if (!inicio.hasNonNull("localDate") || evento.path("dates").path("status").path("code").asText("").equals("cancelled")) {
                continue;
            }
            LocalTime hora = inicio.hasNonNull("localTime") ? LocalTime.parse(inicio.get("localTime").asText()) : HORA_SUPUESTA;
            LocalDateTime comienzo = LocalDate.parse(inicio.get("localDate").asText()).atTime(hora);

            JsonNode sede = evento.path("_embedded").path("venues").path(0);
            Double latitud = numero(sede.path("location").path("latitude"));
            Double longitud = numero(sede.path("location").path("longitude"));
            eventos.add(new EventoCandidato(
                    "tm-" + evento.path("id").asText(),
                    FuentePlan.TICKETMASTER,
                    evento.path("name").asText(),
                    comienzo,
                    comienzo.plus(DURACION_SUPUESTA),
                    sede.path("name").asText(null),
                    latitud,
                    longitud,
                    categoria(evento.path("classifications").path(0).path("segment").path("name").asText("")),
                    evento.path("url").asText(null),
                    true));
        }
        return eventos;
    }

    private static String categoria(String segmento) {
        return switch (segmento) {
            case "Music" -> "MUSICA";
            case "Arts & Theatre" -> "TEATRO";
            case "Sports" -> "DEPORTES";
            case "Family" -> "FAMILIA";
            default -> "EVENTO";
        };
    }

    private static Double numero(JsonNode valor) {
        if (valor.isNumber()) {
            return valor.asDouble();
        }
        try {
            return valor.isTextual() ? Double.valueOf(valor.asText()) : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
