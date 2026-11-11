package com.theroyale.backend.service.plan.fuentes;

// ===== Distancias y tiempos de traslado desde el hotel =====
// Haversine sobre la esfera terrestre; a pie se calcula a 80 m/min, como pide la especificación.
// En auto se estima a ~250 m/min (tráfico de Manhattan) más 4 minutos de espera y estacionamiento.
public final class Distancias {

    public static final double METROS_POR_MINUTO_A_PIE = 80.0;
    public static final double METROS_POR_MINUTO_EN_AUTO = 250.0;
    public static final double CINCO_MILLAS_METROS = 8046.72;

    // Más allá de 25 minutos a pie se sugiere ir en auto aunque el huésped camine sin problema
    public static final int MAXIMO_MINUTOS_A_PIE = 25;

    private static final double RADIO_TIERRA_METROS = 6_371_000.0;

    private Distancias() {
    }

    public static double metros(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * RADIO_TIERRA_METROS * Math.asin(Math.sqrt(a));
    }

    public static int minutosAPie(double metros) {
        return Math.max(1, (int) Math.ceil(metros / METROS_POR_MINUTO_A_PIE));
    }

    public static int minutosEnAuto(double metros) {
        return (int) Math.ceil(metros / METROS_POR_MINUTO_EN_AUTO) + 4;
    }

    // "6 min walk", o "10 min by car" si hay movilidad reducida o el lugar queda lejos
    public static String traslado(double metros, boolean movilidadReducida) {
        int aPie = minutosAPie(metros);
        if (movilidadReducida || aPie > MAXIMO_MINUTOS_A_PIE) {
            return minutosEnAuto(metros) + " min by car";
        }
        return aPie + " min walk";
    }
}
