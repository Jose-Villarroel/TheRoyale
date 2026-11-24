package com.theroyale.backend.repository;

import com.theroyale.backend.model.Temporada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// ===== Temporadas de precios definidas por el admin =====
@Repository
public interface TemporadaRepository extends JpaRepository<Temporada, Long> {

    List<Temporada> findAllByOrderByFechaInicioAsc();
}
