package com.theroyale.backend.repository;

import com.theroyale.backend.model.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByEmail(String email);

    // Usadas para desvincular una entidad de las tablas intermedias admin_* antes de borrarla
    List<Admin> findByHabitacionesAdministradasId(Long habitacionId);

    List<Admin> findByTiposHabitacionAdministradosId(Long tipoHabitacionId);

    List<Admin> findByServiciosAdministradosId(Long servicioId);
}
