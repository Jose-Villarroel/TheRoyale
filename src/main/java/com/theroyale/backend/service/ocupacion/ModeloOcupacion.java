package com.theroyale.backend.service.ocupacion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// ===== Modelo de pronóstico de ocupación entrenado en Python (Docs/modelo/entrenar_modelo.py) =====
// Es una regresión Ridge: la predicción es intercepto + suma(coeficiente x variable). Los
// coeficientes, las métricas y la procedencia de los datos vienen de modelo-ocupacion.json.
@Component
public class ModeloOcupacion {

    private final double intercepto;
    private final Map<String, Double> coeficientes;
    private final List<Integer> anticipaciones;
    private final Map<Integer, Double> maePorAnticipacion;
    private final double maeModelo;
    private final double maeIngenuo;
    private final String algoritmo;
    private final String fuente;
    private final LocalDate entrenadoEl;
    private final int filasEntrenamiento;

    @Autowired
    public ModeloOcupacion(ObjectMapper objectMapper,
                           @Value("${ocupacion.modelo.recurso:modelo/modelo-ocupacion.json}") String recurso) {
        this(leer(objectMapper, recurso));
    }

    ModeloOcupacion(JsonNode json) {
        intercepto = json.get("intercepto").asDouble();
        Map<String, Double> coef = new LinkedHashMap<>();
        json.get("coeficientes").fields().forEachRemaining(e -> coef.put(e.getKey(), e.getValue().asDouble()));
        coeficientes = Collections.unmodifiableMap(coef);

        List<Integer> ls = new ArrayList<>();
        json.get("anticipaciones").forEach(n -> ls.add(n.asInt()));
        anticipaciones = List.copyOf(ls);

        Map<Integer, Double> mae = new LinkedHashMap<>();
        json.get("maePorAnticipacion").fields()
                .forEachRemaining(e -> mae.put(Integer.parseInt(e.getKey()), e.getValue().asDouble()));
        maePorAnticipacion = Collections.unmodifiableMap(mae);

        maeModelo = json.get("maeModelo").asDouble();
        maeIngenuo = json.get("maeIngenuo").asDouble();
        algoritmo = json.path("algoritmo").asText("Ridge");
        fuente = json.path("fuente").asText("");
        entrenadoEl = LocalDate.parse(json.get("entrenadoEl").asText());
        filasEntrenamiento = json.path("filasEntrenamiento").asInt();
    }

    private static JsonNode leer(ObjectMapper objectMapper, String recurso) {
        try (InputStream entrada = new ClassPathResource(recurso).getInputStream()) {
            return objectMapper.readTree(entrada);
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo leer el modelo de ocupación " + recurso, ex);
        }
    }

    // ===== Predicción cruda (sin recortar), igual a la de scikit-learn =====
    public double predecirCrudo(Map<String, Double> variables) {
        double total = intercepto;
        for (Map.Entry<String, Double> variable : variables.entrySet()) {
            total += coeficientes.getOrDefault(variable.getKey(), 0.0) * variable.getValue();
        }
        return total;
    }

    // ===== Ocupación pronosticada entre 0 y 1 =====
    public double predecir(Map<String, Double> variables) {
        return Math.max(0.0, Math.min(1.0, predecirCrudo(variables)));
    }

    // ===== Variables del modelo, armadas igual que en entrenar_modelo.py =====
    // diasAntes: días entre hoy y la noche (0 = esta noche). Se limita a 90, el máximo entrenado.
    public static Map<String, Double> variables(double reservada, long diasAntes, double ocupacionAnioAnterior,
                                                LocalDate noche, String tipo) {
        double anticipacion = Math.min(Math.max(diasAntes, 0), 90) / 90.0;
        Map<String, Double> v = new LinkedHashMap<>();
        v.put("reservada", reservada);
        v.put("anticipacion", anticipacion);
        v.put("reservadaPorAnticipacion", reservada * anticipacion);
        v.put("ocupacionAnioAnterior", ocupacionAnioAnterior);
        v.put("feriado", FeriadosNyc.esAltaDemanda(noche) ? 1.0 : 0.0);
        v.put("dia_" + noche.getDayOfWeek().getValue(), 1.0);
        v.put("mes_" + noche.getMonthValue(), 1.0);
        v.put("tipo_" + tipo, 1.0);
        return v;
    }

    // ===== Error medio esperado según cuánto falta para la noche =====
    // Usa la anticipación entrenada más cercana (0, 7, 14, 30, 60 o 90 días).
    public double errorEsperado(long diasAntes) {
        int cercana = anticipaciones.get(0);
        for (int l : anticipaciones) {
            if (Math.abs(l - diasAntes) < Math.abs(cercana - diasAntes)) {
                cercana = l;
            }
        }
        return maePorAnticipacion.getOrDefault(cercana, maeModelo);
    }

    public double getMaeModelo() {
        return maeModelo;
    }

    public double getMaeIngenuo() {
        return maeIngenuo;
    }

    public Map<Integer, Double> getMaePorAnticipacion() {
        return maePorAnticipacion;
    }

    public String getAlgoritmo() {
        return algoritmo;
    }

    public String getFuente() {
        return fuente;
    }

    // Fecha de referencia del historial: el CSV termina el día anterior a esta fecha
    public LocalDate getEntrenadoEl() {
        return entrenadoEl;
    }

    public int getFilasEntrenamiento() {
        return filasEntrenamiento;
    }
}
