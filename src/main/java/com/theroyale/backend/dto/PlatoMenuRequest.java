package com.theroyale.backend.dto;

import com.theroyale.backend.model.CategoriaPlato;
import com.theroyale.backend.model.EtiquetaPlato;
import com.theroyale.backend.model.RestauranteHotel;

import java.math.BigDecimal;
import java.util.List;

// ===== Datos que envía el admin para crear o editar un plato de la carta =====
// Las validaciones (obligatorios, longitudes, precio > 0) las hace PlatoMenuService.
public record PlatoMenuRequest(
        String nombre,
        String descripcion,
        BigDecimal precio,
        RestauranteHotel restaurante,
        CategoriaPlato categoria,
        List<EtiquetaPlato> etiquetas,
        Boolean disponible,
        String imagenUrl
) {
}
