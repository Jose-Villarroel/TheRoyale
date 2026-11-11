package com.theroyale.backend.service.plan.fuentes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// ===== Eventos con permiso de la ciudad en Manhattan (NYC Open Data, API SODA de Socrata) =====
// Dataset tvpp-9vvx "NYC Permitted Event Information", verificado el 2026-11-11 en
// https://data.cityofnewyork.us/api/views/tvpp-9vvx.json. Campos usados: event_id (number),
// event_name, event_type, event_borough, event_location (text) y start_date_time / end_date_time
// (calendar_date: fecha-hora local sin zona, "2026-12-11T09:00:00.000"). No trae coordenadas ni URL.
// Solo se piden los tipos que un turista puede visitar: se excluyen construcción, filmación
// ("Production Event"), cargas de teatro, partidos deportivos y cierres. Caché de 6 h.
@Component
@ConditionalOnProperty(prefix = "plan.fuentes", name = "habilitadas", havingValue = "true", matchIfMissing = true)
public class NycOpenDataEventosClient implements EventosClient {

    private static final String URL_DATASET = "https://data.cityofnewyork.us/resource/tvpp-9vvx.json";

    // Página pública del dataset: es el enlace "oficial" del evento, que no trae URL propia
    public static final String URL_FICHA = "https://data.cityofnewyork.us/City-Government/NYC-Permitted-Event-Information/tvpp-9vvx";

    static final List<String> TIPOS_PARA_TURISTAS = List.of(
            "Farmers Market", "Street Festival", "Single Block Festival", "Parade",
            "Plaza Event", "Plaza Partner Event", "Sidewalk Sale", "Special Event");

    // "Special Event" incluye permisos privados (bodas, cumpleaños, cierres de césped): se descartan por nombre
    private static final List<String> PALABRAS_EXCLUIDAS = List.of(
            "closure", "closed", "construction", "filming", "film shoot", "production", "load in", "load out",
            "celebration", "birthday", "wedding", "party", "photo", "private", "rehearsal", "memorial service",
            "maintenance", "permit", "parking", "soccer", "softball", "baseball", "basketball", "practice");

    private static final Map<String, String> CATEGORIAS = new HashMap<>();

    static {
        CATEGORIAS.put("Farmers Market", "FERIA");
        CATEGORIAS.put("Street Festival", "FERIA");
        CATEGORIAS.put("Single Block Festival", "FERIA");
        CATEGORIAS.put("Sidewalk Sale", "FERIA");
        CATEGORIAS.put("Parade", "DESFILE");
        CATEGORIAS.put("Plaza Event", "EVENTO");
        CATEGORIAS.put("Plaza Partner Event", "EVENTO");
        CATEGORIAS.put("Special Event", "EVENTO");
    }

    private final ClienteHttpFuente http;
    private final PlanVisitaProperties propiedades;
    private final String urlDataset;
    private final CacheTtl cache = new CacheTtl(Duration.ofHours(6));

    @Autowired
    public NycOpenDataEventosClient(PlanVisitaProperties propiedades, ObjectMapper objectMapper) {
        this(propiedades, objectMapper, URL_DATASET);
    }

    // Para las pruebas: apunta a un servidor local que imita a Socrata
    NycOpenDataEventosClient(PlanVisitaProperties propiedades, ObjectMapper objectMapper, String urlDataset) {
        this.propiedades = propiedades;
        this.urlDataset = urlDataset;
        this.http = new ClienteHttpFuente("NYC Open Data", propiedades.getTimeoutFuenteSegundos(), objectMapper);
    }

    @Override
    public FuentePlan fuente() {
        return FuentePlan.NYC_OPEN_DATA;
    }

    @Override
    public List<EventoCandidato> buscar(LocalDate inicio, LocalDate fin) {
        return cache.obtener(inicio + ":" + fin, () -> {
            Map<String, String> headers = new HashMap<>();
            if (propiedades.getNycOpenData().tieneToken()) {
                headers.put("X-App-Token", propiedades.getNycOpenData().getAppToken());
            }
            return parsear(http.obtenerJson(construirUri(inicio, fin), headers));
        });
    }

    // Eventos que se cruzan con la estadía: empiezan antes de que termine y terminan después de que empiece
    URI construirUri(LocalDate inicio, LocalDate fin) {
        String tipos = String.join(",", TIPOS_PARA_TURISTAS.stream().map(t -> "'" + t + "'").toList());
        String where = "start_date_time <= '" + fin + "T23:59:59' AND end_date_time >= '" + inicio + "T00:00:00'"
                + " AND event_borough = 'Manhattan' AND event_type in(" + tipos + ")";
        return UriComponentsBuilder.fromHttpUrl(urlDataset)
                .queryParam("$where", where)
                .queryParam("$order", "start_date_time")
                .queryParam("$limit", 200)
                .encode()
                .build()
                .toUri();
    }

    static List<EventoCandidato> parsear(JsonNode respuesta) {
        if (!respuesta.isArray()) {
            throw new FuenteNoDisponibleException("NYC Open Data no devolvió una lista de eventos.");
        }
        List<EventoCandidato> eventos = new ArrayList<>();
        for (JsonNode fila : respuesta) {
            String tipo = fila.path("event_type").asText("");
            String nombre = fila.path("event_name").asText("").trim();
            if (!TIPOS_PARA_TURISTAS.contains(tipo) || nombre.isEmpty() || esExcluido(nombre)
                    || !fila.hasNonNull("start_date_time") || !fila.hasNonNull("end_date_time")) {
                continue;
            }
            String ubicacion = fila.path("event_location").asText("");
            double[] coordenadas = UbicacionesManhattan.ubicar(ubicacion);
            eventos.add(new EventoCandidato(
                    "nyc-" + fila.path("event_id").asText(),
                    FuentePlan.NYC_OPEN_DATA,
                    nombre,
                    LocalDateTime.parse(fila.get("start_date_time").asText()),
                    LocalDateTime.parse(fila.get("end_date_time").asText()),
                    lugarLegible(ubicacion),
                    coordenadas == null ? null : coordenadas[0],
                    coordenadas == null ? null : coordenadas[1],
                    CATEGORIAS.getOrDefault(tipo, "EVENTO"),
                    URL_FICHA,
                    false));
        }
        return eventos;
    }

    private static boolean esExcluido(String nombre) {
        String texto = nombre.toLowerCase(Locale.ROOT);
        return PALABRAS_EXCLUIDAS.stream().anyMatch(texto::contains);
    }

    // "Central Park: Wagner Cove   ,Central Park: Cherry Hill" -> "Central Park: Wagner Cove"
    private static String lugarLegible(String ubicacion) {
        String primera = ubicacion.split(",")[0].replaceAll("\\s+", " ").trim();
        return primera.length() > 120 ? primera.substring(0, 120) : primera;
    }
}
