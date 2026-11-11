package com.theroyale.backend.service.plan.fuentes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theroyale.backend.model.TipoClima;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// ===== Pruebas de OpenMeteoClimaClient con respuestas reales guardadas (sin red) =====
// open-meteo-forecast.json: pronóstico de 16 días pedido el 2026-11-11.
// open-meteo-archive.json: histórico 2023-2025 recortado a los días 12 a 15 de diciembre.
class OpenMeteoClimaClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void openMeteoClimaClient_parsearPronostico_devuelve16DiasConCodigoWmo() throws IOException {
        // Arrange
        JsonNode respuesta = objectMapper.readTree(NwsClimaClientTest.leer("open-meteo-forecast.json"));

        // Act
        List<ClimaDia> dias = OpenMeteoClimaClient.parsearPronostico(respuesta);

        // Assert
        assertThat(dias).hasSize(16);
        ClimaDia segundo = dias.get(1);
        assertThat(segundo.fecha()).isEqualTo(LocalDate.of(2026, 11, 12));
        assertThat(segundo.resumen()).isEqualTo("Rain");     // weather_code 63
        assertThat(segundo.maxC()).isEqualTo(14);             // 14.3
        assertThat(segundo.minC()).isEqualTo(8);              // 7.6
        assertThat(segundo.probLluvia()).isEqualTo(85);
        assertThat(segundo.tipo()).isEqualTo(TipoClima.PRONOSTICO_OPEN_METEO);
        assertThat(dias.get(2).resumen()).isEqualTo("Snow"); // weather_code 73
    }

    @Test
    void openMeteoClimaClient_parsearClimaTipico_promediaLosMismosDiasDeTresAnios() throws IOException {
        // Arrange
        JsonNode respuesta = objectMapper.readTree(NwsClimaClientTest.leer("open-meteo-archive.json"));

        // Act
        List<ClimaDia> dias = OpenMeteoClimaClient.parsearClimaTipico(respuesta,
                LocalDate.of(2026, 12, 12), LocalDate.of(2026, 12, 15));

        // Assert
        assertThat(dias).hasSize(4).allMatch(d -> d.tipo() == TipoClima.CLIMA_TIPICO);
        // 12 de diciembre: máximas 6.1, 4.8 y 1.4 -> 4.1; sin lluvia en ninguno de los tres años
        assertThat(dias.get(0).fecha()).isEqualTo(LocalDate.of(2026, 12, 12));
        assertThat(dias.get(0).maxC()).isEqualTo(4);
        assertThat(dias.get(0).probLluvia()).isZero();
        assertThat(dias.get(0).resumen()).isEqualTo("Cold, usually dry");
        // 14 de diciembre: llovió 10.8 mm en 2025 -> 1 de 3 años
        assertThat(dias.get(2).probLluvia()).isEqualTo(33);
    }

    @Test
    void openMeteoClimaClient_respuestaDeError_lanzaFuenteNoDisponible() throws IOException {
        // Arrange: respuesta real del archive-api cuando está saturado (HTTP 429)
        JsonNode respuesta = objectMapper.readTree("{\"error\":true,\"reason\":\"Too many concurrent requests\"}");

        // Act & Assert
        assertThatThrownBy(() -> OpenMeteoClimaClient.parsearClimaTipico(respuesta,
                LocalDate.of(2026, 12, 12), LocalDate.of(2026, 12, 15)))
                .isInstanceOf(FuenteNoDisponibleException.class)
                .hasMessageContaining("Too many concurrent requests");
    }

    @Test
    void descripcionClima_normalDelMes_daClimaTipicoDeCentralPark() {
        // Act
        ClimaDia diciembre = DescripcionClima.normalDelMes(LocalDate.of(2026, 12, 13));

        // Assert
        assertThat(diciembre.tipo()).isEqualTo(TipoClima.CLIMA_TIPICO);
        assertThat(diciembre.maxC()).isEqualTo(7);
        assertThat(diciembre.minC()).isZero();
    }
}
