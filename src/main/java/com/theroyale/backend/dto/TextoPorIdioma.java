package com.theroyale.backend.dto;

import java.util.Locale;

// ===== Elige la versión en español de un texto de la base de datos, o cae al inglés =====
// El inglés es el idioma base: nombre / descripcion siempre tienen valor, mientras que nombreEs /
// descripcionEs son nullables (hoy solo los siembra DataLoader). Un valor en blanco cuenta como
// ausente, para que una cadena vacía guardada por error no deje la tarjeta sin texto.
public final class TextoPorIdioma {

    private TextoPorIdioma() {
    }

    public static String elegir(Locale idioma, String ingles, String espanol) {
        if (idioma != null && "es".equalsIgnoreCase(idioma.getLanguage())
                && espanol != null && !espanol.isBlank()) {
            return espanol;
        }
        return ingles;
    }
}
