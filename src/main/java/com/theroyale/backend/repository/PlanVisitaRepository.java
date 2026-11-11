package com.theroyale.backend.repository;

import com.theroyale.backend.model.EstadoReserva;
import com.theroyale.backend.model.PlanVisita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// ===== Acceso a datos de los planes de visita (uno por reserva) =====
@Repository
public interface PlanVisitaRepository extends JpaRepository<PlanVisita, Long> {

    Optional<PlanVisita> findByReservaId(Long reservaId);

    // Tarea diaria de clima: planes de estadías activas con algún día dentro del rango
    @Query("""
            SELECT p FROM PlanVisita p
            WHERE p.reserva.fechaInicio <= :hasta
              AND p.reserva.fechaFin >= :desde
              AND p.reserva.estado IN :estados
            """)
    List<PlanVisita> buscarConDiasEntre(@Param("desde") LocalDate desde,
                                        @Param("hasta") LocalDate hasta,
                                        @Param("estados") List<EstadoReserva> estados);
}
