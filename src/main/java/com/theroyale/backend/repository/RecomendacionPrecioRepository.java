package com.theroyale.backend.repository;

import com.theroyale.backend.model.EstadoRecomendacion;
import com.theroyale.backend.model.RecomendacionPrecio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// ===== Recomendaciones de precio: bandeja del admin y control de la generación semanal =====
@Repository
public interface RecomendacionPrecioRepository extends JpaRepository<RecomendacionPrecio, Long> {

    List<RecomendacionPrecio> findAllByOrderBySemanaInicioAscTipoHabitacionIdAsc();

    List<RecomendacionPrecio> findByEstadoOrderBySemanaInicioAscTipoHabitacionIdAsc(EstadoRecomendacion estado);

    List<RecomendacionPrecio> findByEstadoAndSemanaInicioLessThanEqual(EstadoRecomendacion estado, LocalDate fecha);

    Optional<RecomendacionPrecio> findFirstByTipoHabitacionIdAndSemanaInicioAndEstado(
            Long tipoHabitacionId, LocalDate semanaInicio, EstadoRecomendacion estado);

    @Query("select max(r.fechaGeneracion) from RecomendacionPrecio r")
    Optional<LocalDateTime> ultimaGeneracion();
}
