package com.theroyale.backend.service.chat;

import java.util.Arrays;
import java.util.List;

// ===== Separa del texto en streaming la línea oculta de sugerencias que escribe el modelo =====
// El system instruction pide terminar cada respuesta con "[[SUGERENCIAS: a | b | c]]" en el idioma
// del cliente. Este filtro deja pasar el texto visible fragmento a fragmento y, en cuanto aparece
// el marcador, deja de reenviar y guarda lo que sigue. Retiene el final de cada fragmento que podría
// ser el comienzo del marcador (por ejemplo "[[SUG"), para no mostrarlo a medias.
// No es un bean: ChatService crea uno por respuesta.
class FiltroSugerencias {

    static final String MARCADOR = "[[SUGERENCIAS:";
    private static final String CIERRE = "]]";
    private static final int MAXIMO_SUGERENCIAS = 3;
    private static final int LARGO_MAXIMO_SUGERENCIA = 60;

    private final StringBuilder pendiente = new StringBuilder();
    private final StringBuilder capturado = new StringBuilder();
    private boolean capturando;
    private boolean huboTextoVisible;

    // Devuelve la parte del fragmento que ya se puede mostrar (puede ser vacía)
    String procesar(String fragmento) {
        if (fragmento == null || fragmento.isEmpty()) {
            return "";
        }
        if (capturando) {
            capturado.append(fragmento);
            return "";
        }

        pendiente.append(fragmento);
        String texto = pendiente.toString();
        int inicioMarcador = texto.indexOf(MARCADOR);
        if (inicioMarcador >= 0) {
            capturando = true;
            capturado.append(texto.substring(inicioMarcador + MARCADOR.length()));
            pendiente.setLength(0);
            return visible(texto.substring(0, inicioMarcador));
        }

        int retenido = largoPrefijoDelMarcadorAlFinal(texto);
        pendiente.setLength(0);
        pendiente.append(texto.substring(texto.length() - retenido));
        return visible(texto.substring(0, texto.length() - retenido));
    }

    // Al terminar la respuesta: lo retenido que al final no era el marcador se muestra
    String finalizar() {
        String resto = pendiente.toString();
        pendiente.setLength(0);
        return visible(resto);
    }

    boolean huboTextoVisible() {
        return huboTextoVisible;
    }

    List<String> sugerencias() {
        String contenido = capturado.toString();
        int cierre = contenido.indexOf(CIERRE);
        if (cierre >= 0) {
            contenido = contenido.substring(0, cierre);
        }
        return Arrays.stream(contenido.split("\\|"))
                .map(String::trim)
                .filter(sugerencia -> !sugerencia.isEmpty() && sugerencia.length() <= LARGO_MAXIMO_SUGERENCIA)
                .limit(MAXIMO_SUGERENCIAS)
                .toList();
    }

    private String visible(String texto) {
        if (!texto.isBlank()) {
            huboTextoVisible = true;
        }
        return texto;
    }

    private int largoPrefijoDelMarcadorAlFinal(String texto) {
        int maximo = Math.min(texto.length(), MARCADOR.length() - 1);
        for (int largo = maximo; largo > 0; largo--) {
            if (MARCADOR.startsWith(texto.substring(texto.length() - largo))) {
                return largo;
            }
        }
        return 0;
    }
}
