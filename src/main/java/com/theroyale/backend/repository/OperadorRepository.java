package com.theroyale.backend.repository;

import com.theroyale.backend.model.Operador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

// ===== Acceso a datos del personal del hotel; findByEmail sostiene el login de operadores =====
@Repository
public interface OperadorRepository extends JpaRepository<Operador, Long> {
    Optional<Operador> findByEmail(String email);

    List<Operador> findAllByOrderByNombreAsc();
}