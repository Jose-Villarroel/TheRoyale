package com.theroyale.backend.dto;

import com.theroyale.backend.model.EstadoRecomendacion;
import com.theroyale.backend.model.RecomendacionPrecio;
import com.theroyale.backend.model.TarifaAplicada;
import com.theroyale.backend.model.Temporada;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// ===== Respuestas de la API de precios dinámicos (temporadas, recomendaciones y tarifas) =====
public final class PreciosDtos {

    private PreciosDtos() {
    }

    public record TemporadaResponse(Long id, String nombre, LocalDate fechaInicio, LocalDate fechaFin, double factor) {
        public static TemporadaResponse from(Temporada t) {
            return new TemporadaResponse(t.getId(), t.getNombre(), t.getFechaInicio(), t.getFechaFin(), t.getFactor());
        }
    }

    public record RecomendacionResponse(Long id, Long tipoHabitacionId, String tipoHabitacionNombre,
                                        LocalDate semanaInicio, LocalDate semanaFin, double factorTemporada,
                                        double ocupacionEsperada, double factorDemanda, double factorActual,
                                        double factorRecomendado, BigDecimal precioBaseReferencia,
                                        BigDecimal precioActual, BigDecimal precioRecomendado, String motivo,
                                        String etiqueta, EstadoRecomendacion estado, LocalDateTime fechaGeneracion,
                                        LocalDateTime fechaDecision, String decididaPor) {
        public static RecomendacionResponse from(RecomendacionPrecio r) {
            return new RecomendacionResponse(r.getId(), r.getTipoHabitacion().getId(), r.getTipoHabitacion().getNombre(),
                    r.getSemanaInicio(), r.getSemanaFin(), r.getFactorTemporada(), r.getOcupacionEsperada(),
                    r.getFactorDemanda(), r.getFactorActual(), r.getFactorRecomendado(), r.getPrecioBaseReferencia(),
                    r.getPrecioActual(), r.getPrecioRecomendado(), r.getMotivo(), r.getEtiqueta(), r.getEstado(),
                    r.getFechaGeneracion(), r.getFechaDecision(), r.getDecididaPor());
        }
    }

    public record TarifaResponse(Long id, Long tipoHabitacionId, String tipoHabitacionNombre, LocalDate fechaInicio,
                                 LocalDate fechaFin, double factor, String etiqueta, String aprobadaPor,
                                 LocalDateTime fechaAprobacion) {
        public static TarifaResponse from(TarifaAplicada t) {
            return new TarifaResponse(t.getId(), t.getTipoHabitacion().getId(), t.getTipoHabitacion().getNombre(),
                    t.getFechaInicio(), t.getFechaFin(), t.getFactor(), t.getEtiqueta(), t.getAprobadaPor(),
                    t.getFechaAprobacion());
        }
    }

    // ultimaGeneracion null = todavía no se generó ninguna
    public record EstadoRecomendacionesResponse(LocalDateTime ultimaGeneracion, LocalDateTime proximaGeneracion,
                                                long pendientes) {
    }
}
