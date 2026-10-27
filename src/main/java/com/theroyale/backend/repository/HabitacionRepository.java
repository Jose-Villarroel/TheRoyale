package com.theroyale.backend.repository;

import com.theroyale.backend.model.EstadoHabitacion;
import com.theroyale.backend.model.EstadoReserva;
import com.theroyale.backend.model.Habitacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

// ===== Acceso a datos de habitaciones físicas, incluida la consulta de disponibilidad por fechas =====
@Repository
public interface HabitacionRepository extends JpaRepository<Habitacion, Long> {
    // JpaRepository provee: findAll(), findById(), save(), deleteById(), etc.
    boolean existsByTipoHabitacionId(Long tipoHabitacionId);

    // reservaIdExcluida permite que una reserva no se bloquee a si misma al modificarla; null = no excluir nada
    @Query("""
            SELECT h FROM Habitacion h
            WHERE h.tipoHabitacion.id = :tipoHabitacionId
              AND h.estado NOT IN :estadosNoUtilizables
              AND NOT EXISTS (
                    SELECT r FROM Reserva r
                    WHERE r.habitacion = h
                      AND r.estado IN :estadosBloqueantes
                      AND r.fechaInicio < :fechaFin
                      AND r.fechaFin > :fechaInicio
                      AND (:reservaIdExcluida IS NULL OR r.id <> :reservaIdExcluida)
              )
            ORDER BY h.numero
            """)


   List<Habitacion> buscarDisponibles(
            @Param("tipoHabitacionId") Long tipoHabitacionId,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin,
            @Param("estadosNoUtilizables") Collection<EstadoHabitacion> estadosNoUtilizables,
            @Param("estadosBloqueantes") Collection<EstadoReserva> estadosBloqueantes,
            @Param("reservaIdExcluida") Long reservaIdExcluida);
    List<Habitacion> findByTipoHabitacionIdOrderByNumeroAsc(Long tipoHabitacionId);

    Optional<Habitacion> findByNumero(String numero);

    List<Habitacion> findAllByOrderByNumeroAsc();

    long countByEstado(EstadoHabitacion estado);
}
