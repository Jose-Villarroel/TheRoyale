package com.theroyale.backend.dto;

import com.theroyale.backend.model.Imagen;
import com.theroyale.backend.model.TipoHabitacion;
import java.util.List;

public record TipoHabitacionResponseDto(
        Long id,
        String nombre,
        String descripcion,
        String imagenUrl,
        List<String> imagenes
) {
    public static TipoHabitacionResponseDto from(TipoHabitacion tipo) {
        if (tipo == null) return null;
        List<String> urls = (tipo.getImagenes() != null)
                ? tipo.getImagenes().stream().map(Imagen::getUrl).toList()
                : List.of();
        return new TipoHabitacionResponseDto(
                tipo.getId(),
                tipo.getNombre(),
                tipo.getDescripcion(),
                tipo.getImagenUrl(),
                urls
        );
    }
}
