package com.theroyale.backend.service.plan.fuentes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theroyale.backend.config.HotelProperties;
import com.theroyale.backend.config.PlanVisitaProperties;
import com.theroyale.backend.model.FuentePlan;
import com.theroyale.backend.model.TipoClima;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// ===== Pronóstico oficial del National Weather Service para los próximos 7 días =====
// Dos pasos: GET /points/{lat},{lon} devuelve properties.forecast (la URL de la cuadrícula) y ese
// forecast trae periodos de 12 h (día y noche). La API exige un User-Agent que identifique la app.
// Caché de 1 h (los pronósticos de NWS se actualizan varias veces al día).
@Component
@ConditionalOnProperty(prefix = "plan.fuentes", name = "habilitadas", havingValue = "true", matchIfMissing = true)
public class NwsClimaClient implements ClimaClient {

    private static final String URL_BASE = "https://api.weather.gov";

    private final ClienteHttpFuente http;
    private final HotelProperties hotel;
    private final PlanVisitaProperties propiedades;
    private final String urlBase;
    private final CacheTtl cache = new CacheTtl(Duration.ofHours(1));

    @Autowired
    public NwsClimaClient(HotelProperties hotel, PlanVisitaProperties propiedades, ObjectMapper objectMapper) {
        this(hotel, propiedades, objectMapper, URL_BASE);
    }

    // Para las pruebas: apunta a un servidor local que imita a NWS
    NwsClimaClient(HotelProperties hotel, PlanVisitaProperties propiedades, ObjectMapper objectMapper, String urlBase) {
        this.hotel = hotel;
        this.propiedades = propiedades;
        this.urlBase = urlBase;
        this.http = new ClienteHttpFuente("NWS", propiedades.getTimeoutFuenteSegundos(), objectMapper);
    }

    @Override
    public FuentePlan fuente() {
        return FuentePlan.NWS;
    }

    @Override
    public List<ClimaDia> pronostico(LocalDate inicio, LocalDate fin) {
        List<ClimaDia> todos = cache.obtener("pronostico", this::consultar);
        return todos.stream()
                .filter(dia -> !dia.fecha().isBefore(inicio) && !dia.fecha().isAfter(fin))
                .toList();
    }

    private List<ClimaDia> consultar() {
        Map<String, String> headers = Map.of(
                "User-Agent", propiedades.getNws().getUserAgent(),
                "Accept", "application/geo+json");
        URI puntos = URI.create(String.format(Locale.US, "%s/points/%.4f,%.4f",
                urlBase, hotel.getLatitud(), hotel.getLongitud()));
        String urlPronostico = cache.obtener("urlPronostico", () -> {
            JsonNode respuesta = http.obtenerJson(puntos, headers);
            JsonNode url = respuesta.path("properties").path("forecast");
            if (!url.isTextual()) {
                throw new FuenteNoDisponibleException("NWS no devolvió la URL del pronóstico.");
            }
            return url.asText();
        });
        return parsearPronostico(http.obtenerJson(URI.create(urlPronostico), headers));
    }

    // Agrupa los periodos por fecha local: el de día da la máxima y el resumen, el de noche la mínima.
    // La probabilidad de lluvia del día es la mayor de los dos periodos.
    static List<ClimaDia> parsearPronostico(JsonNode respuesta) {
        JsonNode periodos = respuesta.path("properties").path("periods");
        if (!periodos.isArray() || periodos.isEmpty()) {
            throw new FuenteNoDisponibleException("NWS no devolvió periodos de pronóstico.");
        }

        Map<LocalDate, JsonNode[]> porFecha = new LinkedHashMap<>();
        for (JsonNode periodo : periodos) {
            LocalDate fecha = OffsetDateTime.parse(periodo.path("startTime").asText()).toLocalDate();
            JsonNode[] diaYNoche = porFecha.computeIfAbsent(fecha, f -> new JsonNode[2]);
            diaYNoche[periodo.path("isDaytime").asBoolean() ? 0 : 1] = periodo;
        }

        List<ClimaDia> dias = new ArrayList<>();
        porFecha.forEach((fecha, diaYNoche) -> {
            JsonNode dia = diaYNoche[0];
            JsonNode noche = diaYNoche[1];
            JsonNode principal = dia != null ? dia : noche;
            int max = celsius(principal);
            int min = noche != null ? celsius(noche) : max;
            int lluvia = Math.max(probabilidad(dia), probabilidad(noche));
            dias.add(new ClimaDia(fecha, principal.path("shortForecast").asText(""),
                    Math.max(max, min), Math.min(max, min), lluvia, TipoClima.PRONOSTICO_NWS));
        });
        return dias;
    }

    private static int celsius(JsonNode periodo) {
        double temperatura = periodo.path("temperature").asDouble();
        return "C".equalsIgnoreCase(periodo.path("temperatureUnit").asText("F"))
                ? (int) Math.round(temperatura)
                : DescripcionClima.fahrenheitACelsius(temperatura);
    }

    private static int probabilidad(JsonNode periodo) {
        if (periodo == null) {
            return 0;
        }
        JsonNode valor = periodo.path("probabilityOfPrecipitation").path("value");
        return valor.isNumber() ? valor.asInt() : 0;
    }
}
