package com.theroyale.backend.repository;

import com.theroyale.backend.model.ReservaHistorica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// ===== Acceso al historial simulado de reservas (pronóstico de ocupación) =====
@Repository
public interface ReservaHistoricaRepository extends JpaRepository<ReservaHistorica, Long> {
}
