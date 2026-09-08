package com.theroyale.backend.repository;

import com.theroyale.backend.model.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ServicioRepository extends JpaRepository<Servicio, Long> {
    // JpaRepository provee: findAll(), findById(), save(), deleteById(), etc.
    Optional<Servicio> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);
}
