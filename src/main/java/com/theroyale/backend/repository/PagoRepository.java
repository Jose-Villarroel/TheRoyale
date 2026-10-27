package com.theroyale.backend.repository;

import com.theroyale.backend.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// ===== Acceso a datos de pagos =====
// Los pagos se crean desde Cuenta (cascade); aquí solo se consulta si un operador tiene pagos registrados.
@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

    boolean existsByOperadorId(Long operadorId);
}
