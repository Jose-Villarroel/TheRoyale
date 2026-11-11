package com.theroyale.backend.service.plan.fuentes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theroyale.backend.config.PlanVisitaProperties;
import com.theroyale.backend.model.FuentePlan;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// ===== Pruebas de NycOpenDataEventosClient con filas reales del dataset tvpp-9vvx (sin red) =====
// El fixture mezcla eventos para turistas con permisos que se deben descartar: celebraciones privadas,
// filmaciones ("Production Event"), eventos corporativos ("Street Event"), cierres de césped y fútbol.
class NycOpenDataEventosClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void nycOpenDataEventosClient_parsear_soloDejaEventosVisitablesConFechasYUbicacion() throws IOException {
        // Arrange
        JsonNode respuesta = objectMapper.readTree(NwsClimaClientTest.leer("nyc-open-data-eventos.json"));

        // Act
        List<EventoCandidato> eventos = NycOpenDataEventosClient.parsear(respuesta);

        // Assert
        assertThat(eventos).extracting(EventoCandidato::titulo).containsExactlyInAnyOrder(
                "Santa's Knights and Gladiators NYC", "South Arabia", "Columbia Thursday Sunday Greenmarket");
        assertThat(eventos).allMatch(e -> e.fuente() == FuentePlan.NYC_OPEN_DATA && e.id().startsWith("nyc-"));

        EventoCandidato santa = eventos.stream().filter(e -> e.titulo().startsWith("Santa")).findFirst().orElseThrow();
        assertThat(santa.inicio()).isEqualTo(LocalDateTime.of(2026, 12, 12, 12, 0));
        assertThat(santa.lugar()).isEqualTo("Central Park: Lawn Behind the Met Museum");
        assertThat(santa.latitud()).isEqualTo(40.7812);   // parque conocido
        assertThat(santa.url()).isEqualTo(NycOpenDataEventosClient.URL_FICHA);
        assertThat(santa.bajoTecho()).isFalse();

        EventoCandidato mercado = eventos.stream().filter(e -> e.titulo().contains("Greenmarket")).findFirst().orElseThrow();
        assertThat(mercado.categoria()).isEqualTo("FERIA");
        assertThat(mercado.latitud()).isNotNull();        // estimada por la calle 113
    }

    @Test
    void nycOpenDataEventosClient_construirUri_usaLosCamposRealesDelDataset() {
        // Arrange
        NycOpenDataEventosClient cliente = new NycOpenDataEventosClient(new PlanVisitaProperties(), objectMapper);

        // Act
        String uri = URLDecoder.decode(cliente.construirUri(LocalDate.of(2026, 12, 12), LocalDate.of(2026, 12, 15))
                .toString(), StandardCharsets.UTF_8);

        // Assert
        assertThat(uri).startsWith("https://data.cityofnewyork.us/resource/tvpp-9vvx.json?");
        assertThat(uri).contains("start_date_time <= '2026-12-15T23:59:59'")
                .contains("end_date_time >= '2026-12-12T00:00:00'")
                .contains("event_borough = 'Manhattan'")
                .contains("'Farmers Market'")
                .doesNotContain("'Production Event'")
                .contains("$limit=200");
    }

    @Test
    void nycOpenDataEventosClient_parsear_respuestaQueNoEsLista_lanzaFuenteNoDisponible() throws IOException {
        // Arrange: Socrata responde un objeto con "error" cuando la consulta es inválida
        JsonNode respuesta = objectMapper.readTree("{\"error\":true,\"message\":\"Invalid SoQL query\"}");

        // Act & Assert
        assertThatThrownBy(() -> NycOpenDataEventosClient.parsear(respuesta))
                .isInstanceOf(FuenteNoDisponibleException.class);
    }

    @Test
    void ubicacionesManhattan_ubicar_estimaPorCalleYDevuelveNullSiNoReconoce() {
        // Act
        double[] calle51 = UbicacionesManhattan.ubicar("WEST 51 STREET between 5 AVENUE and 6 AVENUE");
        double[] desconocida = UbicacionesManhattan.ubicar("PEARL STREET between WATER STREET and FULTON");

        // Assert
        assertThat(calle51).containsExactly(40.7590, -73.9772);
        assertThat(desconocida).isNull();
    }
}
