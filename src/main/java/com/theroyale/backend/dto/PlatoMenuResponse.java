package com.theroyale.backend.dto;

import com.theroyale.backend.model.CategoriaPlato;
import com.theroyale.backend.model.EtiquetaPlato;
import com.theroyale.backend.model.PlatoMenu;
import com.theroyale.backend.model.RestauranteHotel;

import java.math.BigDecimal;
import java.util.List;

// ===== Plato de la carta tal como lo consumen el admin del menú y las tarjetas del chat =====
// Los enums salen por su nombre (SIGNATURE_RESTAURANT, PRINCIPAL, VEGANO...).
public record PlatoMenuResponse(
        Long id,
        String nombre,
        String descripcion,
        BigDecimal precio,
        RestauranteHotel restaurante,
        CategoriaPlato categoria,
        List<EtiquetaPlato> etiquetas,
        boolean disponible,
        String imagenUrl
) {

    public static PlatoMenuResponse from(PlatoMenu plato) {
        return new PlatoMenuResponse(
                plato.getId(),
                plato.getNombre(),
                plato.getDescripcion(),
                plato.getPrecio(),
                plato.getRestaurante(),
                plato.getCategoria(),
                plato.getEtiquetas().stream().sorted().toList(),
                plato.isDisponible(),
                plato.getImagenUrl()
        );
    }
}
