package com.theroyale.backend.repository;

import com.theroyale.backend.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    // JpaRepository provee: findAll(), findById(), save(), deleteById(), etc.
    Optional<Cliente> findByEmailIgnoreCase(String email);
}
