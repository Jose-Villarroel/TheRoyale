package com.theroyale.backend.repository;

import com.theroyale.backend.model.TarifaAplicada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// ===== Precios aprobados por el admin =====
@Repository
public interface TarifaAplicadaRepository extends JpaRepository<TarifaAplicada, Long> {

    List<TarifaAplicada> findAllByOrderByFechaInicioAscTipoHabitacionIdAsc();

    List<TarifaAplicada> findByTipoHabitacionId(Long tipoHabitacionId);
}
