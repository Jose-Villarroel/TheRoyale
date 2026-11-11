package com.theroyale.backend.service.chat;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

// ===== Sugerencias de respaldo cuando el modelo no escribió la línea [[SUGERENCIAS: ...]] =====
// Se eligen de forma determinista según la herramienta usada en la respuesta, en español si el
// mensaje del cliente parece español y en inglés en cualquier otro caso.
final class SugerenciasPredeterminadas {

    private static final Set<String> PALABRAS_ESPANOL = Set.of(
            "hola", "que", "hay", "tienen", "tiene", "para", "habitacion", "habitaciones", "precio", "precios",
            "quiero", "cuanto", "cuesta", "gracias", "platos", "comida", "noche", "noches", "puedo", "donde",
            "como", "cual", "cuales", "disponible", "disponibilidad", "reservar", "una", "los", "las", "del",
            "por", "favor", "buenas", "buenos", "dias", "tardes", "mascotas", "ninos", "desayuno", "veganos");

    private SugerenciasPredeterminadas() {
    }

    static List<String> para(Set<String> herramientasUsadas, String mensajeCliente) {
        boolean espanol = pareceEspanol(mensajeCliente);
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

    static boolean pareceEspanol(String texto) {
        if (texto == null || texto.isBlank()) {
            return false;
        }
        if (texto.contains("¿") || texto.contains("¡") || texto.toLowerCase(Locale.ROOT).contains("ñ")) {
            return true;
        }
        String sinTildes = Normalizer.normalize(texto.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return Arrays.stream(sinTildes.split("[^a-z]+")).anyMatch(PALABRAS_ESPANOL::contains);
    }
}
