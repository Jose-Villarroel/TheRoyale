package com.theroyale.backend.repository;

import com.theroyale.backend.model.Cuenta;
import com.theroyale.backend.model.EstadoCuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// ===== Acceso a datos de cuentas de consumo =====
// Se buscan por reserva porque la relación es 1 a 1 y el resto del sistema solo conoce la reserva.
@Repository
public interface CuentaRepository extends JpaRepository<Cuenta, Long> {

    Optional<Cuenta> findByReservaId(Long reservaId);

    long countByEstado(EstadoCuenta estado);
}
