package com.theroyale.backend.repository;

import com.theroyale.backend.model.Imagen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// ===== Acceso a datos de las imágenes de galería de los tipos de habitación =====
// Sin consultas propias: las imágenes se gestionan en cascada desde TipoHabitacion.
@Repository
public interface ImagenRepository extends JpaRepository<Imagen, Long> {
}