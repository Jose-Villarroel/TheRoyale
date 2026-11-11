package com.theroyale.backend.service.plan.fuentes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theroyale.backend.config.HotelProperties;
import com.theroyale.backend.config.PlanVisitaProperties;
import com.theroyale.backend.model.FuentePlan;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// ===== Pruebas de TicketmasterEventosClient (sin red y sin clave) =====
// ticketmaster-eventos.json está hecho a mano con la forma documentada de Discovery API v2.
class TicketmasterEventosClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void ticketmasterEventosClient_parsear_normalizaEventosYDescartaCancelados() throws IOException {
        // Arrange
        JsonNode respuesta = objectMapper.readTree(NwsClimaClientTest.leer("ticketmaster-eventos.json"));

        // Act
        List<EventoCandidato> eventos = TicketmasterEventosClient.parsear(respuesta);

        // Assert
        assertThat(eventos).hasSize(3);
        EventoCandidato concierto = eventos.get(0);
        assertThat(concierto.id()).isEqualTo("tm-vvG1zZ9KbK8aaa");
        assertThat(concierto.fuente()).isEqualTo(FuentePlan.TICKETMASTER);
        assertThat(concierto.titulo()).isEqualTo("Holiday Concert at Carnegie Hall");
        assertThat(concierto.inicio()).isEqualTo(LocalDateTime.of(2026, 12, 12, 19, 30));
        assertThat(concierto.fin()).isEqualTo(LocalDateTime.of(2026, 12, 12, 22, 30)); // 3 h supuestas
        assertThat(concierto.lugar()).isEqualTo("Carnegie Hall");
        assertThat(concierto.latitud()).isEqualTo(40.765);
        assertThat(concierto.longitud()).isEqualTo(-73.97996);
        assertThat(concierto.categoria()).isEqualTo("MUSICA");
        assertThat(concierto.url()).startsWith("https://www.ticketmaster.com/");
        assertThat(concierto.bajoTecho()).isTrue();

        assertThat(eventos.get(1).categoria()).isEqualTo("TEATRO");
        // Sin localTime se supone una función a las 19:00
        assertThat(eventos.get(2).inicio()).isEqualTo(LocalDateTime.of(2026, 12, 14, 19, 0));
        assertThat(eventos.get(2).categoria()).isEqualTo("DEPORTES");
    }

    @Test
    void ticketmasterEventosClient_parsear_sinResultados_devuelveListaVacia() throws IOException {
        // Arrange: sin eventos Ticketmaster responde solo "page"
        JsonNode respuesta = objectMapper.readTree("{\"page\":{\"size\":50,\"totalElements\":0}}");

        // Act & Assert
        assertThat(TicketmasterEventosClient.parsear(respuesta)).isEmpty();
    }

    @Test
    void ticketmasterEventosClient_buscar_sinClave_quedaApagadoSinSalirAInternet() {
        // Arrange
        TicketmasterEventosClient cliente = new TicketmasterEventosClient(new HotelProperties(),
                new PlanVisitaProperties(), objectMapper, "http://localhost:1/no-se-llama");

        // Act & Assert
        assertThatThrownBy(() -> cliente.buscar(LocalDate.of(2026, 12, 12), LocalDate.of(2026, 12, 15)))
                .isInstanceOf(FuenteNoDisponibleException.class)
                .hasMessageContaining("TICKETMASTER_API_KEY");
    }

    @Test
    void ticketmasterEventosClient_construirUri_pideCincoMillasAlrededorDelHotelEnUtc() {
        // Arrange
        PlanVisitaProperties propiedades = new PlanVisitaProperties();
        propiedades.getTicketmaster().setApiKey("clave-falsa");
        TicketmasterEventosClient cliente = new TicketmasterEventosClient(new HotelProperties(), propiedades, objectMapper);

        // Act
        String uri = cliente.construirUri(LocalDate.of(2026, 12, 12), LocalDate.of(2026, 12, 15)).toString();

        // Assert: medianoche de Nueva York (UTC-5 en diciembre) = 05:00Z
        assertThat(uri).startsWith("https://app.ticketmaster.com/discovery/v2/events.json?")
                .contains("latlong=40.7590,-73.9772")
                .contains("radius=5").contains("unit=miles")
                .contains("startDateTime=2026-12-12T05:00:00Z")
                .contains("endDateTime=2026-12-16T04:59:59Z")
                .contains("size=50");
    }

    @Test
    void clienteHttpFuente_ocultarClaves_noDejaLaClaveEnElMensaje() {
        // Act
        String mensaje = ClienteHttpFuente.ocultarClaves(
                "I/O error on GET request for \"https://app.ticketmaster.com/x.json?apikey=SECRETA&size=50\"");

        // Assert
        assertThat(mensaje).doesNotContain("SECRETA").contains("apikey=***&size=50");
    }
}
