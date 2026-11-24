package com.theroyale.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

// ===== Idioma de la respuesta: lo decide el header Accept-Language de la petición =====
// El inglés es el idioma base y el español es la alternativa (ver docs/i18n-codigos-error.md).
// El resolver deja el Locale en LocaleContextHolder y de ahí lo leen ApiExceptionHandler (para
// resolver el mensaje en MessageSource), los DTO de respuesta (nombreEs / descripcionEs) y el chat.
//
// supportedLocales = [en, es] + defaultLocale = ENGLISH:
// - "Accept-Language: es" o "es-CO" -> español;
// - "Accept-Language: fr" (un idioma que no está en la lista) -> inglés, no francés;
// - sin header -> inglés.
// Lo segundo es lo que no hace AcceptHeaderLocaleResolver sin supportedLocales: devolvería fr.
@Configuration
public class I18nConfig {

    // Locale.of() es de Java 19; el proyecto compila con 17, así que se usa el constructor.
    @SuppressWarnings("deprecation")
    public static final Locale ESPANOL = new Locale("es");

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.ENGLISH);
        resolver.setSupportedLocales(List.of(Locale.ENGLISH, ESPANOL));
        return resolver;
    }
}
