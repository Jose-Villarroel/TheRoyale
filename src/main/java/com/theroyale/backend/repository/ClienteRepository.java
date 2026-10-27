package com.theroyale.backend.repository;

import com.theroyale.backend.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// ===== Acceso a datos de clientes; las búsquedas por email y cédula sostienen login y registro =====
@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    // JpaRepository provee: findAll(), findById(), save(), deleteById(), etc.
    Optional<Cliente> findByEmailIgnoreCase(String email);

    Optional<Cliente> findByCedula(String cedula);
}
