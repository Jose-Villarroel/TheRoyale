package com.theroyale.backend.repository;

import com.theroyale.backend.model.TipoHabitacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TipoHabitacionRepository extends JpaRepository<TipoHabitacion, Long> {
    // JpaRepository provee: findAll(), findById(), save(), deleteById(), etc.
}