package com.theroyale.backend.repository;

import com.theroyale.backend.model.EstadoHabitacion;
import com.theroyale.backend.model.Habitacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HabitacionRepository extends JpaRepository<Habitacion, Long> {
    // JpaRepository provee: findAll(), findById(), save(), deleteById(), etc.
    boolean existsByTipoHabitacionId(Long tipoHabitacionId);

    List<Habitacion> findByTipoHabitacionIdOrderByNumeroAsc(Long tipoHabitacionId);

    Optional<Habitacion> findByNumero(String numero);

    List<Habitacion> findAllByOrderByNumeroAsc();

    long countByEstado(EstadoHabitacion estado);
}
