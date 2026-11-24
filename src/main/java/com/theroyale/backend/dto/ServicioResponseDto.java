package com.theroyale.backend.dto;

import com.theroyale.backend.model.Servicio;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.List;
import java.util.Locale;

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
        return from(servicio, LocaleContextHolder.getLocale());
    }

    public static ServicioResponseDto from(Servicio servicio, Locale idioma) {
        if (servicio == null) return null;
        return new ServicioResponseDto(
                servicio.getId(),
                TextoPorIdioma.elegir(idioma, servicio.getNombre(), servicio.getNombreEs()),
                TextoPorIdioma.elegir(idioma, servicio.getDescripcion(), servicio.getDescripcionEs()),
                servicio.getPrecio(),
                servicio.getImagenUrl(),
                servicio.getCaracteristicas(),
                servicio.getGaleriaUrls()
        );
    }
}
