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
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// ===== Open-Meteo: pronóstico de 16 días y clima típico (promedio de los 3 años anteriores) =====
// Sin clave. El pronóstico se usa para los días 8 a 16 (los 7 primeros los cubre NWS); para fechas
// más lejanas se pide el histórico (archive-api) de los mismos días de los 3 años anteriores en una
// sola llamada y se promedia. Caché de 6 h.
@Component
@ConditionalOnProperty(prefix = "plan.fuentes", name = "habilitadas", havingValue = "true", matchIfMissing = true)
public class OpenMeteoClimaClient implements ClimaClient {

    private static final String URL_PRONOSTICO = "https://api.open-meteo.com/v1/forecast";
    private static final String URL_HISTORICO = "https://archive-api.open-meteo.com/v1/archive";
    private static final int ANIOS_HISTORICO = 3;

    // Un día cuenta como lluvioso en el histórico si llovió al menos 1 mm
    private static final double MM_DIA_LLUVIOSO = 1.0;

    private final ClienteHttpFuente http;
    private final HotelProperties hotel;
    private final String urlPronostico;
    private final String urlHistorico;
    private final CacheTtl cache = new CacheTtl(Duration.ofHours(6));

    @Autowired
    public OpenMeteoClimaClient(HotelProperties hotel, PlanVisitaProperties propiedades, ObjectMapper objectMapper) {
        this(hotel, propiedades, objectMapper, URL_PRONOSTICO, URL_HISTORICO);
    }

    // Para las pruebas: apunta a un servidor local que imita a Open-Meteo
    OpenMeteoClimaClient(HotelProperties hotel, PlanVisitaProperties propiedades, ObjectMapper objectMapper,
                         String urlPronostico, String urlHistorico) {
        this.hotel = hotel;
        this.urlPronostico = urlPronostico;
        this.urlHistorico = urlHistorico;
        this.http = new ClienteHttpFuente("Open-Meteo", propiedades.getTimeoutFuenteSegundos(), objectMapper);
    }

    @Override
    public FuentePlan fuente() {
        return FuentePlan.OPEN_METEO;
    }

    @Override
    public List<ClimaDia> pronostico(LocalDate inicio, LocalDate fin) {
        List<ClimaDia> todos = cache.obtener("pronostico", () -> parsearPronostico(http.obtenerJson(URI.create(
                String.format(Locale.US, "%s?latitude=%.4f&longitude=%.4f"
                                + "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max"
                                + "&timezone=America%%2FNew_York&forecast_days=16",
                        urlPronostico, hotel.getLatitud(), hotel.getLongitud())), Map.of())));
        return todos.stream()
                .filter(dia -> !dia.fecha().isBefore(inicio) && !dia.fecha().isAfter(fin))
                .toList();
    }

    @Override
    public List<ClimaDia> climaTipico(LocalDate inicio, LocalDate fin) {
        LocalDate desde = inicio.minusYears(ANIOS_HISTORICO);
        LocalDate hasta = fin.minusYears(1);
        return cache.obtener("tipico:" + inicio + ":" + fin, () -> parsearClimaTipico(http.obtenerJson(URI.create(
                String.format(Locale.US, "%s?latitude=%.4f&longitude=%.4f&start_date=%s&end_date=%s"
                                + "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum"
                                + "&timezone=America%%2FNew_York",
                        urlHistorico, hotel.getLatitud(), hotel.getLongitud(), desde, hasta)), Map.of()),
                inicio, fin));
    }

    static List<ClimaDia> parsearPronostico(JsonNode respuesta) {
        JsonNode diario = diario(respuesta);
        JsonNode fechas = diario.path("time");
        List<ClimaDia> dias = new ArrayList<>();
        for (int i = 0; i < fechas.size(); i++) {
            JsonNode max = diario.path("temperature_2m_max").path(i);
            JsonNode min = diario.path("temperature_2m_min").path(i);
            if (!max.isNumber() || !min.isNumber()) {
                continue;
            }
            JsonNode lluvia = diario.path("precipitation_probability_max").path(i);
            dias.add(new ClimaDia(LocalDate.parse(fechas.get(i).asText()),
                    DescripcionClima.desdeCodigoWmo(diario.path("weather_code").path(i).asInt()),
                    (int) Math.round(max.asDouble()), (int) Math.round(min.asDouble()),
                    lluvia.isNumber() ? lluvia.asInt() : null, TipoClima.PRONOSTICO_OPEN_METEO));
        }
        return dias;
    }

    // Para cada fecha de la estadía promedia los mismos día y mes de los años del histórico.
    // probLluvia = porcentaje de esos años en que llovió al menos 1 mm (0, 33, 67 o 100).
    static List<ClimaDia> parsearClimaTipico(JsonNode respuesta, LocalDate inicio, LocalDate fin) {
        JsonNode diario = diario(respuesta);
        JsonNode fechas = diario.path("time");

        Map<MonthDay, List<double[]>> porDiaDelAnio = new HashMap<>();
        for (int i = 0; i < fechas.size(); i++) {
            JsonNode max = diario.path("temperature_2m_max").path(i);
            JsonNode min = diario.path("temperature_2m_min").path(i);
            if (!max.isNumber() || !min.isNumber()) {
                continue;
            }
            double lluvia = diario.path("precipitation_sum").path(i).asDouble(0);
            porDiaDelAnio.computeIfAbsent(MonthDay.from(LocalDate.parse(fechas.get(i).asText())), k -> new ArrayList<>())
                    .add(new double[]{max.asDouble(), min.asDouble(), lluvia >= MM_DIA_LLUVIOSO ? 1 : 0});
        }

        List<ClimaDia> dias = new ArrayList<>();
        for (LocalDate fecha = inicio; !fecha.isAfter(fin); fecha = fecha.plusDays(1)) {
            List<double[]> muestras = porDiaDelAnio.get(MonthDay.from(fecha));
            if (muestras == null || muestras.isEmpty()) {
                continue;
            }
            int max = (int) Math.round(muestras.stream().mapToDouble(m -> m[0]).average().orElse(0));
            int min = (int) Math.round(muestras.stream().mapToDouble(m -> m[1]).average().orElse(0));
            int lluvia = (int) Math.round(100 * muestras.stream().mapToDouble(m -> m[2]).average().orElse(0));
            dias.add(new ClimaDia(fecha, DescripcionClima.resumenTipico(max, lluvia), max, min, lluvia,
                    TipoClima.CLIMA_TIPICO));
        }
        return dias;
    }

    private static JsonNode diario(JsonNode respuesta) {
        if (respuesta.path("error").asBoolean(false)) {
            throw new FuenteNoDisponibleException("Open-Meteo respondió con error: " + respuesta.path("reason").asText());
        }
        JsonNode diario = respuesta.path("daily");
        if (!diario.path("time").isArray()) {
            throw new FuenteNoDisponibleException("Open-Meteo no devolvió datos diarios.");
        }
        return diario;
    }
}
