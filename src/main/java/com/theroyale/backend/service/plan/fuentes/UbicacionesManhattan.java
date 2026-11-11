package com.theroyale.backend.service.plan.fuentes;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// ===== Coordenadas aproximadas para los eventos de NYC Open Data, que no traen latitud ni longitud =====
// El campo event_location trae textos como "Central Park: Dene Lawn", "Bryant Park: Fountain Terrace"
// o "WEST 43 STREET between 6 AVENUE and 7 AVENUE". Primero se busca un parque o plaza conocido; si no,
// se estima la posición por el número de calle sobre la cuadrícula de Manhattan (14th a 220th St.,
// con un error de hasta ~1 km). Si no se reconoce, el evento queda sin coordenadas.
public final class UbicacionesManhattan {

    private static final Map<String, double[]> CONOCIDAS = new LinkedHashMap<>();

    static {
        CONOCIDAS.put("central park", new double[]{40.7812, -73.9665});
        CONOCIDAS.put("bryant park", new double[]{40.7536, -73.9832});
        CONOCIDAS.put("union square", new double[]{40.7359, -73.9911});
        CONOCIDAS.put("madison square park", new double[]{40.7420, -73.9880});
        CONOCIDAS.put("washington square park", new double[]{40.7308, -73.9973});
        CONOCIDAS.put("battery park", new double[]{40.7033, -74.0170});
        CONOCIDAS.put("the battery", new double[]{40.7033, -74.0170});
        CONOCIDAS.put("hudson river park", new double[]{40.7270, -74.0110});
        CONOCIDAS.put("riverside park", new double[]{40.8010, -73.9720});
        CONOCIDAS.put("dag hammarskjold plaza", new double[]{40.7527, -73.9690});
        CONOCIDAS.put("times square", new double[]{40.7580, -73.9855});
        CONOCIDAS.put("rockefeller", new double[]{40.7587, -73.9787});
        CONOCIDAS.put("herald square", new double[]{40.7500, -73.9878});
        CONOCIDAS.put("columbus circle", new double[]{40.7681, -73.9819});
        CONOCIDAS.put("lincoln center", new double[]{40.7725, -73.9835});
        CONOCIDAS.put("high line", new double[]{40.7480, -74.0048});
        CONOCIDAS.put("tompkins square park", new double[]{40.7265, -73.9817});
        CONOCIDAS.put("city hall park", new double[]{40.7128, -74.0060});
        CONOCIDAS.put("foley square", new double[]{40.7143, -74.0030});
        CONOCIDAS.put("bowling green", new double[]{40.7049, -74.0137});
    }

    private static final Pattern CALLE_NUMERADA =
            Pattern.compile("\\b(?:EAST|WEST|E|W)\\s+(\\d{1,3})\\s+(?:STREET|ST)\\b", Pattern.CASE_INSENSITIVE);

    private UbicacionesManhattan() {
    }

    public static double[] ubicar(String ubicacion) {
        if (ubicacion == null || ubicacion.isBlank()) {
            return null;
        }
        String texto = ubicacion.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, double[]> conocida : CONOCIDAS.entrySet()) {
            if (texto.contains(conocida.getKey())) {
                return conocida.getValue();
            }
        }
        Matcher calle = CALLE_NUMERADA.matcher(ubicacion);
        if (calle.find()) {
            int numero = Integer.parseInt(calle.group(1));
            if (numero >= 14 && numero <= 220) {
                // Sobre la Quinta Avenida: 51st St queda en 40.7590, -73.9772 (el hotel); cada calle suma
                // ~0.000624 de latitud y ~0.00044 de longitud por el giro de 29° de la cuadrícula
                return new double[]{40.7590 + (numero - 51) * 0.000624, -73.9772 + (numero - 51) * 0.00044};
            }
        }
        return null;
    }
}
