package com.theroyale.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// ===== Configuración del chatbot (propiedades gemini.* de application.properties) =====
// La clave NUNCA se escribe en el código ni en los .properties: llega solo por la variable de
// entorno GEMINI_API_KEY (gemini.api-key=${GEMINI_API_KEY:}). Sin clave el chat queda "offline".
@Component
@ConfigurationProperties(prefix = "gemini")
@Getter
@Setter
public class GeminiProperties {

    private String apiKey = "";

    private String modelo = "gemini-3.1-flash-lite";

    // Modelos que se prueban, en orden, si el principal falla antes de responder (404 porque Google lo
    // retiró, 429/503 por saturación o un timeout). Google cambia la oferta de modelos con frecuencia.
    private List<String> modelosRespaldo = new ArrayList<>();

    // Turnos del historial (mensajes previos) que se envían al modelo; los más antiguos se descartan
    private int maxTurnos = 10;

    // Tiempo máximo sin recibir datos de Gemini antes de abortar (y pasar al modelo de respaldo)
    private int timeoutSegundos = 20;

    public boolean tieneClave() {
        return apiKey != null && !apiKey.isBlank();
    }

    // El principal primero y luego los de respaldo, sin vacíos ni repetidos
    public List<String> modelosEnOrden() {
        Set<String> modelos = new LinkedHashSet<>();
        if (modelo != null && !modelo.isBlank()) {
            modelos.add(modelo.trim());
        }
        for (String respaldo : modelosRespaldo) {
            if (respaldo != null && !respaldo.isBlank()) {
                modelos.add(respaldo.trim());
            }
        }
        return List.copyOf(modelos);
    }

    // Evita que la clave aparezca en logs si alguien imprime este objeto
    @Override
    public String toString() {
        return "GeminiProperties{modelos=" + modelosEnOrden() + ", maxTurnos=" + maxTurnos
                + ", timeoutSegundos=" + timeoutSegundos + ", clave=" + (tieneClave() ? "***" : "none") + "}";
    }
}
