package com.theroyale.backend.dto;

import com.theroyale.backend.model.Imagen;
import com.theroyale.backend.model.TipoHabitacion;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.List;
import java.util.Locale;

public record TipoHabitacionResponseDto(
        Long id,
        String nombre,
        String descripcion,
        String imagenUrl,
        List<String> imagenes
) {
    // Idioma de la peticion (AcceptHeaderLocaleResolver, ver I18nConfig)
    public static TipoHabitacionResponseDto from(TipoHabitacion tipo) {
        return from(tipo, LocaleContextHolder.getLocale());
    }

    public static TipoHabitacionResponseDto from(TipoHabitacion tipo, Locale idioma) {
        if (tipo == null) return null;
        List<String> urls = (tipo.getImagenes() != null)
                ? tipo.getImagenes().stream().map(Imagen::getUrl).toList()
                : List.of();
        return new TipoHabitacionResponseDto(
                tipo.getId(),
                TextoPorIdioma.elegir(idioma, tipo.getNombre(), tipo.getNombreEs()),
                TextoPorIdioma.elegir(idioma, tipo.getDescripcion(), tipo.getDescripcionEs()),
                tipo.getImagenUrl(),
                urls
        );
    }
}
