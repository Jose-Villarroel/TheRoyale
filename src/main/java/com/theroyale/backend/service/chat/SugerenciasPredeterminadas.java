package com.theroyale.backend.service.chat;

import java.util.List;
import java.util.Locale;

// ===== Sugerencias de respaldo cuando el modelo no escribió la línea [[SUGERENCIAS: ...]] =====
// Se eligen de forma determinista según la herramienta usada en la respuesta, y en el idioma de la
// petición: el Locale lo resuelve AcceptHeaderLocaleResolver (I18nConfig) a partir de Accept-Language.
//
// Antes el idioma se adivinaba con una lista de palabras españolas sobre el mensaje del huésped
// ("hola", "precio", "gracias"...). Eso fallaba con mensajes cortos o con palabras compartidas por
// los dos idiomas, y además no coincidía con el idioma que el huésped eligió en la interfaz.
final class SugerenciasPredeterminadas {

    private SugerenciasPredeterminadas() {
    }

    static List<String> para(java.util.Set<String> herramientasUsadas, Locale idioma) {
        boolean espanol = esEspanol(idioma);
        if (herramientasUsadas.contains(HerramientasChatService.CONSULTAR_DISPONIBILIDAD)) {
            return espanol
                    ? List.of("Ver el menú del restaurante", "¿Qué ofrece el spa?", "Política de cancelación")
                    : List.of("Show the dining menu", "What does the spa offer?", "Cancellation policy");
        }
        if (herramientasUsadas.contains(HerramientasChatService.BUSCAR_PLATOS)) {
            return espanol
                    ? List.of("¿Hay opciones veganas?", "Horarios de los restaurantes", "Consultar disponibilidad")
                    : List.of("Any vegan options?", "Restaurant opening hours", "Check room availability");
        }
        return espanol
                ? List.of("Consultar disponibilidad", "Ver el menú del restaurante", "Servicios del hotel")
                : List.of("Check room availability", "Show the dining menu", "Hotel services");
    }

    // Cualquier variante regional ("es", "es-CO", "es-419") cuenta como español; el resto, inglés
    static boolean esEspanol(Locale idioma) {
        return idioma != null && "es".equalsIgnoreCase(idioma.getLanguage());
    }
}
