package com.theroyale.backend.service.plan;

import com.theroyale.backend.model.RestauranteHotel;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

// ===== Horarios de los restaurantes del hotel (los mismos de chatbot/informacion-hotel.md) =====
// Breakfast Lounge 06:30-10:30 (11:00 los fines de semana); Signature Restaurant 12:00-15:00 y
// 18:00-23:00; Craft Cocktail Bar 17:00-01:00 (para el plan, hasta las 23:59); Room Service 24 h.
public final class HorariosRestaurantes {

    public record Franja(LocalTime inicio, LocalTime fin) {

        public boolean contiene(LocalTime desde, LocalTime hasta) {
            return !desde.isBefore(inicio) && !hasta.isAfter(fin);
        }
    }

    private static final LocalTime FIN_DEL_DIA = LocalTime.of(23, 59);

    private HorariosRestaurantes() {
    }

    public static List<Franja> franjas(RestauranteHotel restaurante, LocalDate dia) {
        return switch (restaurante) {
            case BREAKFAST_LOUNGE -> List.of(new Franja(LocalTime.of(6, 30),
                    esFinDeSemana(dia) ? LocalTime.of(11, 0) : LocalTime.of(10, 30)));
            case SIGNATURE_RESTAURANT -> List.of(new Franja(LocalTime.of(12, 0), LocalTime.of(15, 0)),
                    new Franja(LocalTime.of(18, 0), LocalTime.of(23, 0)));
            case COCKTAIL_BAR -> List.of(new Franja(LocalTime.of(17, 0), FIN_DEL_DIA));
            case ROOM_SERVICE -> List.of(new Franja(LocalTime.MIDNIGHT, FIN_DEL_DIA));
        };
    }

    public static boolean abierto(RestauranteHotel restaurante, LocalDate dia, LocalTime desde, LocalTime hasta) {
        return franjas(restaurante, dia).stream().anyMatch(franja -> franja.contiene(desde, hasta));
    }

    public static String nombre(RestauranteHotel restaurante) {
        return switch (restaurante) {
            case SIGNATURE_RESTAURANT -> "Signature Restaurant";
            case COCKTAIL_BAR -> "Craft Cocktail Bar";
            case ROOM_SERVICE -> "24h Room Service";
            case BREAKFAST_LOUNGE -> "Breakfast Lounge";
        };
    }

    public static String horario(RestauranteHotel restaurante) {
        return switch (restaurante) {
            case SIGNATURE_RESTAURANT -> "Lunch 12:00-15:00, dinner 18:00-23:00";
            case COCKTAIL_BAR -> "Daily 17:00-01:00";
            case ROOM_SERVICE -> "24 hours, delivered to the suite";
            case BREAKFAST_LOUNGE -> "Daily 06:30-10:30 (until 11:00 on weekends)";
        };
    }

    public static String categoria(RestauranteHotel restaurante) {
        return switch (restaurante) {
            case SIGNATURE_RESTAURANT, ROOM_SERVICE -> "DINING";
            case COCKTAIL_BAR -> "BAR";
            case BREAKFAST_LOUNGE -> "BREAKFAST";
        };
    }

    private static boolean esFinDeSemana(LocalDate dia) {
        return dia.getDayOfWeek() == DayOfWeek.SATURDAY || dia.getDayOfWeek() == DayOfWeek.SUNDAY;
    }
}
