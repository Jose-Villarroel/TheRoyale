package com.theroyale.backend.service.ocupacion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

// ===== Pruebas del modelo de ocupación exportado desde Python =====
// modelo-ocupacion.json trae 3 filas de ejemplo con la predicción que dio scikit-learn: Java debe
// dar exactamente lo mismo (si no, el modelo del panel no sería el que se entrenó).
class ModeloOcupacionTest {

    private static JsonNode json() throws Exception {
        try (InputStream entrada = new ClassPathResource("modelo/modelo-ocupacion.json").getInputStream()) {
            return new ObjectMapper().readTree(entrada);
        }
    }

    @Test
    void predecirCrudo_daLoMismoQueScikitLearnEnLosEjemplos() throws Exception {
        JsonNode json = json();
        ModeloOcupacion modelo = new ModeloOcupacion(json);

        assertThat(json.get("ejemplos")).hasSize(3);
        for (JsonNode ejemplo : json.get("ejemplos")) {
            Map<String, Double> variables = new LinkedHashMap<>();
            ejemplo.get("variables").fields().forEachRemaining(e -> variables.put(e.getKey(), e.getValue().asDouble()));
            assertThat(modelo.predecirCrudo(variables)).isCloseTo(ejemplo.get("prediccion").asDouble(), within(1e-5));
        }
    }

    @Test
    void elModeloSuperaAlIngenuo() throws Exception {
        ModeloOcupacion modelo = new ModeloOcupacion(json());
        assertThat(modelo.getMaeModelo()).isLessThan(modelo.getMaeIngenuo());
    }

    @Test
    void predecir_siempreQuedaEntreCeroYUno() throws Exception {
        ModeloOcupacion modelo = new ModeloOcupacion(json());
        LocalDate navidad = LocalDate.of(2026, 12, 24);

        double lleno = modelo.predecir(ModeloOcupacion.variables(1.0, 0, 1.0, navidad, "Normal"));
        double vacio = modelo.predecir(ModeloOcupacion.variables(0.0, 90, 0.0, LocalDate.of(2027, 1, 12), "Presidential Suite"));

        assertThat(lleno).isBetween(0.0, 1.0);
        assertThat(vacio).isBetween(0.0, 1.0);
        assertThat(lleno).isGreaterThan(vacio);
    }

    @Test
    void variables_seArmanComoEnPython() {
        Map<String, Double> v = ModeloOcupacion.variables(0.6, 45, 0.7, LocalDate.of(2026, 12, 24), "VIP");

        assertThat(v).containsEntry("reservada", 0.6)
                .containsEntry("anticipacion", 0.5)
                .containsEntry("reservadaPorAnticipacion", 0.3)
                .containsEntry("ocupacionAnioAnterior", 0.7)
                .containsEntry("feriado", 1.0)
                .containsEntry("dia_4", 1.0)   // jueves
                .containsEntry("mes_12", 1.0)
                .containsEntry("tipo_VIP", 1.0);
        // Más de 90 días se trata como 90, el máximo entrenado
        assertThat(ModeloOcupacion.variables(0, 200, 0, LocalDate.of(2027, 3, 1), "VIP")).containsEntry("anticipacion", 1.0);
    }

    @Test
    void errorEsperado_usaLaAnticipacionMasCercana() throws Exception {
        ModeloOcupacion modelo = new ModeloOcupacion(json());
        assertThat(modelo.errorEsperado(1)).isEqualTo(modelo.getMaePorAnticipacion().get(0));
        assertThat(modelo.errorEsperado(80)).isEqualTo(modelo.getMaePorAnticipacion().get(90));
    }

    @Test
    void feriados_coincidenConLosDelGenerador() {
        assertThat(FeriadosNyc.esAltaDemanda(LocalDate.of(2026, 11, 26))).isTrue();  // Thanksgiving 2026
        assertThat(FeriadosNyc.esAltaDemanda(LocalDate.of(2026, 11, 24))).isTrue();  // 2 días antes
        assertThat(FeriadosNyc.esAltaDemanda(LocalDate.of(2026, 11, 29))).isFalse(); // 3 días después
        assertThat(FeriadosNyc.esAltaDemanda(LocalDate.of(2026, 12, 20))).isTrue();
        assertThat(FeriadosNyc.esAltaDemanda(LocalDate.of(2027, 1, 1))).isTrue();
        assertThat(FeriadosNyc.esAltaDemanda(LocalDate.of(2027, 1, 2))).isFalse();
        assertThat(FeriadosNyc.esAltaDemanda(LocalDate.of(2027, 7, 4))).isTrue();
    }
}
