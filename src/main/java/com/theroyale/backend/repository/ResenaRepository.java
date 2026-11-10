package com.theroyale.backend.repository;

import com.theroyale.backend.model.Resena;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResenaRepository extends JpaRepository<Resena, Long> {

    List<Resena> findByTipoHabitacionIdOrderByFechaDesc(Long tipoHabitacionId);

    List<Resena> findAllByOrderByFechaDesc();

    @Query("SELECT AVG(r.estrellas) FROM Resena r WHERE r.tipoHabitacion.id = :tipoHabitacionId")
    Double calcularPromedioEstrellas(@Param("tipoHabitacionId") Long tipoHabitacionId);

    long countByTipoHabitacionId(Long tipoHabitacionId);
}
