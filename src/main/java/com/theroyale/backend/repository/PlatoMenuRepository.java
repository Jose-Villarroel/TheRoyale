package com.theroyale.backend.repository;

import com.theroyale.backend.model.CategoriaPlato;
import com.theroyale.backend.model.EtiquetaPlato;
import com.theroyale.backend.model.PlatoMenu;
import com.theroyale.backend.model.RestauranteHotel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

// ===== Acceso a datos de la carta de los restaurantes =====
// Un solo método de búsqueda: cada filtro en null se ignora, así la API y el chat comparten la consulta.
// Los enums se guardan como texto, por lo que el orden es alfabético por su nombre.
@Repository
public interface PlatoMenuRepository extends JpaRepository<PlatoMenu, Long> {

    @Query("""
            SELECT DISTINCT p FROM PlatoMenu p
            WHERE (:restaurante IS NULL OR p.restaurante = :restaurante)
              AND (:categoria IS NULL OR p.categoria = :categoria)
              AND (:etiqueta IS NULL OR :etiqueta MEMBER OF p.etiquetas)
              AND (:soloDisponibles = false OR p.disponible = true)
            ORDER BY p.restaurante, p.categoria, p.nombre
            """)
    List<PlatoMenu> buscar(@Param("restaurante") RestauranteHotel restaurante,
                           @Param("categoria") CategoriaPlato categoria,
                           @Param("etiqueta") EtiquetaPlato etiqueta,
                           @Param("soloDisponibles") boolean soloDisponibles);
}
