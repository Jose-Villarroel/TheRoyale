package com.theroyale.backend.dto;

import com.theroyale.backend.model.Servicio;
import java.util.List;

public record ServicioResponseDto(
        Long id,
        String nombre,
        String descripcion,
        double precio,
        String imagenUrl,
        List<String> caracteristicas,
        List<String> galeriaUrls
) {
    public static ServicioResponseDto from(Servicio servicio) {
        if (servicio == null) return null;
        return new ServicioResponseDto(
                servicio.getId(),
                servicio.getNombre(),
                servicio.getDescripcion(),
                servicio.getPrecio(),
                servicio.getImagenUrl(),
                servicio.getCaracteristicas(),
                servicio.getGaleriaUrls()
        );
    }
}
