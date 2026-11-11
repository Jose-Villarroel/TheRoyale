package com.theroyale.backend.repository;

import com.theroyale.backend.model.LugarNyc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// ===== Acceso a datos del catálogo curado de lugares de Nueva York =====
@Repository
public interface LugarNycRepository extends JpaRepository<LugarNyc, Long> {

    List<LugarNyc> findByActivoTrueOrderByIdAsc();

    Optional<LugarNyc> findByNombre(String nombre);
}
