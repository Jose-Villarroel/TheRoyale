package com.theroyale.backend.repository;

import com.theroyale.backend.model.Cuenta;
import com.theroyale.backend.model.EstadoCuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CuentaRepository extends JpaRepository<Cuenta, Long> {

    Optional<Cuenta> findByReservaId(Long reservaId);

    long countByEstado(EstadoCuenta estado);
}
