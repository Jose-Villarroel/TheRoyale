package com.theroyale.backend.repository;

import com.theroyale.backend.model.Habitacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HabitacionRepository extends JpaRepository<Habitacion, Long> {
    // JpaRepository provee: findAll(), findById(), save(), deleteById(), etc.
}
