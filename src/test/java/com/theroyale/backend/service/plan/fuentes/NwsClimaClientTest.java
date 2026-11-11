package com.theroyale.backend.service.plan.fuentes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.theroyale.backend.config.HotelProperties;
import com.theroyale.backend.config.PlanVisitaProperties;
import com.theroyale.backend.model.TipoClima;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// ===== Pruebas de NwsClimaClient con respuestas reales guardadas (src/test/resources/plan/nws-*.json) =====
// Sin red: el parseo se prueba directo y los dos pasos (/points y forecast) contra un servidor local.
class NwsClimaClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer servidor;
    private final List<String> rutas = new ArrayList<>();
    private final List<String> userAgents = new ArrayList<>();

    @BeforeEach
    void setUp() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        int puerto = servidor.getAddress().getPort();
        servidor.createContext("/", intercambio -> {
            String ruta = intercambio.getRequestURI().toString();
            rutas.add(ruta);
            userAgents.add(intercambio.getRequestHeaders().getFirst("User-Agent"));
            String cuerpo = ruta.startsWith("/points/")
                    // La URL del pronóstico se reescribe para que el segundo paso también llegue al servidor local
                    ? leer("nws-points.json").replace("https://api.weather.gov", "http://localhost:" + puerto)
                    : leer("nws-forecast.json");
            byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/geo+json");
            intercambio.sendResponseHeaders(200, bytes.length);
            try (OutputStream salida = intercambio.getResponseBody()) {
                salida.write(bytes);
            }
        });
        servidor.start();
    }

    @AfterEach
    void tearDown() {
        servidor.stop(0);
    }

    @Test
    void nwsClimaClient_parsearPronostico_agrupaDiaYNocheEnCelsius() throws IOException {
        // Arrange
        JsonNode respuesta = objectMapper.readTree(leer("nws-forecast.json"));

        // Act
        List<ClimaDia> dias = NwsClimaClient.parsearPronostico(respuesta);

        // Assert: 14 periodos de 12 h = 7 días (el 11 de noviembre empieza de día)
        assertThat(dias).hasSize(7);
        ClimaDia primero = dias.get(0);
        assertThat(primero.fecha()).isEqualTo(LocalDate.of(2026, 11, 11));
        assertThat(primero.resumen()).isEqualTo("Mostly Sunny");
        assertThat(primero.maxC()).isEqualTo(17);   // 63 °F
        assertThat(primero.minC()).isEqualTo(11);   // 51 °F (Tonight)
        assertThat(primero.probLluvia()).isEqualTo(18); // la mayor entre el día (0) y la noche (18)
        assertThat(primero.tipo()).isEqualTo(TipoClima.PRONOSTICO_NWS);

        ClimaDia lluvioso = dias.get(1);
        assertThat(lluvioso.fecha()).isEqualTo(LocalDate.of(2026, 11, 12));
        assertThat(lluvioso.resumen()).isEqualTo("Rain");
        assertThat(lluvioso.probLluvia()).isEqualTo(98);
    }

    @Test
    void nwsClimaClient_parsearPronostico_sinPeriodos_lanzaFuenteNoDisponible() throws IOException {
        // Arrange
        JsonNode respuesta = objectMapper.readTree("{\"properties\":{\"periods\":[]}}");

        // Act & Assert
        assertThatThrownBy(() -> NwsClimaClient.parsearPronostico(respuesta))
                .isInstanceOf(FuenteNoDisponibleException.class);
    }

    @Test
    void nwsClimaClient_pronostico_haceLosDosPasosConUserAgentYUsaLaCache() {
        // Arrange
        PlanVisitaProperties propiedades = new PlanVisitaProperties();
        NwsClimaClient cliente = new NwsClimaClient(new HotelProperties(), propiedades, objectMapper,
                "http://localhost:" + servidor.getAddress().getPort());

        // Act: dos consultas seguidas (la segunda sale de la caché de 1 h)
        List<ClimaDia> dias = cliente.pronostico(LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 13));
        cliente.pronostico(LocalDate.of(2026, 11, 14), LocalDate.of(2026, 11, 15));

        // Assert
        assertThat(dias).extracting(ClimaDia::fecha)
                .containsExactly(LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 13));
        assertThat(rutas).containsExactly("/points/40.7590,-73.9772", "/gridpoints/OKX/34,44/forecast");
        assertThat(userAgents).allMatch("TheRoyaleCourseProject/1.0 (course project)"::equals);
    }

    static String leer(String archivo) throws IOException {
        try (InputStream entrada = NwsClimaClientTest.class.getResourceAsStream("/plan/" + archivo)) {
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
