package com.theroyale.backend.service.plan.fuentes;

import com.theroyale.backend.model.TipoClima;

import java.time.LocalDate;

// ===== Utilidades de clima: códigos WMO de Open-Meteo, °F a °C y normales de Central Park =====
public final class DescripcionClima {

    // Normales climáticas 1991-2020 de NOAA para Central Park (máxima y mínima media por mes, °C).
    // Respaldo cuando Open-Meteo no responde: el día sigue teniendo un clima típico razonable.
    private static final int[][] NORMALES_CENTRAL_PARK = {
            {4, -3}, {6, -1}, {10, 2}, {17, 8}, {22, 13}, {27, 18},
            {29, 21}, {29, 21}, {25, 17}, {18, 10}, {12, 5}, {7, 0}
    };

    // Probabilidad aproximada de un día con lluvia según las normales (unos 10-11 días al mes)
    private static final int PROBABILIDAD_LLUVIA_NORMAL = 35;

    private DescripcionClima() {
    }

    public static int fahrenheitACelsius(double fahrenheit) {
        return (int) Math.round((fahrenheit - 32) * 5.0 / 9.0);
    }

    // Códigos de tiempo WMO que usa Open-Meteo (weather_code)
    public static String desdeCodigoWmo(int codigo) {
        if (codigo == 0) {
            return "Clear";
        }
        if (codigo <= 2) {
            return "Partly cloudy";
        }
        if (codigo == 3) {
            return "Cloudy";
        }
        if (codigo == 45 || codigo == 48) {
            return "Fog";
        }
        if (codigo >= 51 && codigo <= 57) {
            return "Drizzle";
        }
        if (codigo >= 61 && codigo <= 67) {
            return "Rain";
        }
        if (codigo >= 71 && codigo <= 77) {
            return "Snow";
        }
        if (codigo >= 80 && codigo <= 82) {
            return "Rain showers";
        }
        if (codigo == 85 || codigo == 86) {
            return "Snow showers";
        }
        if (codigo >= 95) {
            return "Thunderstorms";
        }
        return "Mixed conditions";
    }

    // Resumen para el clima típico: "Cold, usually dry", "Mild, often rainy"...
    public static String resumenTipico(int maxC, int probLluvia) {
        String temperatura;
        if (maxC <= 2) {
            temperatura = "Very cold";
        } else if (maxC <= 9) {
            temperatura = "Cold";
        } else if (maxC <= 16) {
            temperatura = "Cool";
        } else if (maxC <= 25) {
            temperatura = "Mild";
        } else {
            temperatura = "Warm";
        }
        String lluvia = probLluvia >= 60 ? "often rainy" : probLluvia >= 34 ? "some chance of rain" : "usually dry";
        return temperatura + ", " + lluvia;
    }

    public static ClimaDia normalDelMes(LocalDate fecha) {
        int[] normal = NORMALES_CENTRAL_PARK[fecha.getMonthValue() - 1];
        return new ClimaDia(fecha, resumenTipico(normal[0], PROBABILIDAD_LLUVIA_NORMAL),
                normal[0], normal[1], PROBABILIDAD_LLUVIA_NORMAL, TipoClima.CLIMA_TIPICO);
    }
}
