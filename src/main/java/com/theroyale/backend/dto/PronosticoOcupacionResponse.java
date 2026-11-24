package com.theroyale.backend.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// ===== Pronóstico de ocupación para el panel del admin =====
// Todas las ocupaciones van de 0 a 1. tipoHabitacionId null = hotel completo.
// serie: los últimos días con su ocupación real y los próximos con lo reservado y lo pronosticado.
public record PronosticoOcupacionResponse(
        Long tipoHabitacionId,
        String alcance,
        List<TipoResumen> tipos,
        List<Punto> serie,
        Kpis kpis,
        List<FechaAltaDemanda> altaDemanda,
        FichaModelo modelo
) {

    public record TipoResumen(Long id, String nombre, int habitaciones) {
    }

    // real: solo en días pasados. reservada/pronostico/minimo/maximo: solo en días futuros.
    public record Punto(LocalDate fecha, Double real, Double reservada, Double pronostico,
                        Double minimo, Double maximo) {
    }

    public record Kpis(double pronostico7Dias, double pronostico30Dias, double reservada30Dias,
                       double real30DiasAnteriores) {
    }

    public record FechaAltaDemanda(LocalDate fecha, double pronostico, boolean feriado) {
    }

    public record FichaModelo(String algoritmo, LocalDate entrenadoEl, String fuente, double maeModelo,
                              double maeIngenuo, Map<Integer, Double> maePorAnticipacion, int filasEntrenamiento) {
    }
}
