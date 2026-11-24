package com.theroyale.backend.service.ocupacion;

import com.theroyale.backend.model.EstadoReserva;
import com.theroyale.backend.model.Reserva;
import com.theroyale.backend.model.ReservaHistorica;
import com.theroyale.backend.model.TipoHabitacion;
import com.theroyale.backend.repository.HabitacionRepository;
import com.theroyale.backend.repository.ReservaHistoricaRepository;
import com.theroyale.backend.repository.ReservaRepository;
import com.theroyale.backend.repository.TipoHabitacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// ===== Ocupación por tipo de habitación y noche =====
// Junta el historial simulado (ReservaHistorica) con las reservas reales de la aplicación.
// - Ocupación final de una noche: reservas no canceladas que la cubren / habitaciones del tipo.
// - "Reservada" a una fecha de corte: reservas hechas hasta ese día que cubren la noche, incluidas
//   las que después se cancelaron (en ese momento seguían en el libro). Es la misma definición con
//   que se entrenó el modelo en Python.
@Service
@Transactional(readOnly = true)
public class OcupacionService {

    // Una reserva reducida a lo que importa para contar ocupación
    record Estancia(LocalDate creada, LocalDate llegada, LocalDate salida, boolean cancelada) {
        boolean cubre(LocalDate noche) {
            return !noche.isBefore(llegada) && noche.isBefore(salida);
        }
    }

    private final ReservaHistoricaRepository reservaHistoricaRepository;
    private final ReservaRepository reservaRepository;
    private final HabitacionRepository habitacionRepository;
    private final TipoHabitacionRepository tipoHabitacionRepository;

    // El historial no cambia mientras la app corre: se lee una vez y se guarda agrupado por tipo
    private volatile Map<Long, List<Estancia>> historicoPorTipo;

    public OcupacionService(ReservaHistoricaRepository reservaHistoricaRepository,
                            ReservaRepository reservaRepository, HabitacionRepository habitacionRepository,
                            TipoHabitacionRepository tipoHabitacionRepository) {
        this.reservaHistoricaRepository = reservaHistoricaRepository;
        this.reservaRepository = reservaRepository;
        this.habitacionRepository = habitacionRepository;
        this.tipoHabitacionRepository = tipoHabitacionRepository;
    }

    // ===== Foto de la ocupación: historial + reservas reales, lista para consultar muchas noches =====
    public Foto foto() {
        Map<Long, List<Estancia>> porTipo = new HashMap<>();
        historico().forEach((tipoId, lista) -> porTipo.put(tipoId, new ArrayList<>(lista)));
        for (Reserva reserva : reservaRepository.findAll()) {
            Long tipoId = reserva.getHabitacion().getTipoHabitacion().getId();
            porTipo.computeIfAbsent(tipoId, k -> new ArrayList<>()).add(new Estancia(
                    reserva.getFechaCreacion().toLocalDate(), reserva.getFechaInicio(), reserva.getFechaFin(),
                    reserva.getEstado() == EstadoReserva.CANCELADA));
        }
        Map<Long, Integer> habitaciones = new HashMap<>();
        for (TipoHabitacion tipo : tipoHabitacionRepository.findAll()) {
            habitaciones.put(tipo.getId(), habitacionRepository.findByTipoHabitacionIdOrderByNumeroAsc(tipo.getId()).size());
        }
        return new Foto(porTipo, habitaciones);
    }

    // Lo llama HistoricoOcupacionService después de cargar el CSV
    void olvidarHistorico() {
        historicoPorTipo = null;
    }

    private Map<Long, List<Estancia>> historico() {
        Map<Long, List<Estancia>> cache = historicoPorTipo;
        if (cache == null) {
            cache = new HashMap<>();
            for (ReservaHistorica r : reservaHistoricaRepository.findAll()) {
                cache.computeIfAbsent(r.getTipoHabitacion().getId(), k -> new ArrayList<>())
                        .add(new Estancia(r.getFechaCreacion(), r.getLlegada(), r.getSalida(), r.isCancelada()));
            }
            historicoPorTipo = cache;
        }
        return cache;
    }

    // ===== Consultas sobre una foto ya armada =====
    public static final class Foto {
        private final Map<Long, List<Estancia>> porTipo;
        private final Map<Long, Integer> habitaciones;

        Foto(Map<Long, List<Estancia>> porTipo, Map<Long, Integer> habitaciones) {
            this.porTipo = porTipo;
            this.habitaciones = habitaciones;
        }

        public int habitaciones(Long tipoId) {
            return habitaciones.getOrDefault(tipoId, 0);
        }

        // Fracción ocupada al final (0 a 1) por reservas no canceladas
        public double ocupacionFinal(Long tipoId, LocalDate noche) {
            int total = habitaciones(tipoId);
            if (total == 0) {
                return 0.0;
            }
            long ocupadas = porTipo.getOrDefault(tipoId, List.of()).stream()
                    .filter(e -> !e.cancelada() && e.cubre(noche))
                    .count();
            return Math.min(1.0, (double) ocupadas / total);
        }

        // Fracción reservada para "noche" con reservas hechas hasta "corte" (incluye canceladas después)
        public double reservada(Long tipoId, LocalDate noche, LocalDate corte) {
            int total = habitaciones(tipoId);
            if (total == 0) {
                return 0.0;
            }
            long reservadas = porTipo.getOrDefault(tipoId, List.of()).stream()
                    .filter(e -> !e.creada().isAfter(corte) && e.cubre(noche))
                    .count();
            return Math.min(1.0, (double) reservadas / total);
        }

        // Lo que hoy ocupa de verdad una noche futura: reservas vigentes (no canceladas) hechas hasta hoy
        public double reservadaVigente(Long tipoId, LocalDate noche, LocalDate hoy) {
            int total = habitaciones(tipoId);
            if (total == 0) {
                return 0.0;
            }
            long reservadas = porTipo.getOrDefault(tipoId, List.of()).stream()
                    .filter(e -> !e.cancelada() && !e.creada().isAfter(hoy) && e.cubre(noche))
                    .count();
            return Math.min(1.0, (double) reservadas / total);
        }
    }
}
