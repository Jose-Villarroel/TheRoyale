package com.theroyale.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// ===== Configuración del plan de visita (propiedades plan.* de application.properties) =====
// Las claves NUNCA se escriben en el código ni en los .properties: llegan por variables de entorno
// (TICKETMASTER_API_KEY, NYC_OPEN_DATA_APP_TOKEN) o por el .env. Sin clave de Ticketmaster esa
// fuente queda apagada y el plan se arma con las demás.
@Component
@ConfigurationProperties(prefix = "plan")
@Getter
@Setter
public class PlanVisitaProperties {

    // Tiempo máximo que se espera a cada fuente externa; si no responde, el plan se arma sin ella
    private int timeoutFuenteSegundos = 5;

    // Generaciones (generar + ajustar + regenerar día) permitidas por reserva y por día
    private int maxGeneracionesDia = 5;

    private Fuentes fuentes = new Fuentes();
    private Nws nws = new Nws();
    private Ticketmaster ticketmaster = new Ticketmaster();
    private NycOpenData nycOpenData = new NycOpenData();

    @Getter
    @Setter
    public static class Fuentes {
        // false: se usan las fuentes falsas con datos fijos (también en el perfil test)
        private boolean habilitadas = true;
    }

    @Getter
    @Setter
    public static class Nws {
        // La API de NWS exige un User-Agent que identifique la aplicación
        private String userAgent = "TheRoyaleCourseProject/1.0 (course project)";
    }

    @Getter
    @Setter
    public static class Ticketmaster {
        private String apiKey = "";

        public boolean tieneClave() {
            return apiKey != null && !apiKey.isBlank();
        }

        @Override
        public String toString() {
            return "Ticketmaster{clave=" + (tieneClave() ? "***" : "none") + "}";
        }
    }

    @Getter
    @Setter
    public static class NycOpenData {
        // Opcional: sin token Socrata aplica un límite de uso más bajo, suficiente para el proyecto
        private String appToken = "";

        public boolean tieneToken() {
            return appToken != null && !appToken.isBlank();
        }

        @Override
        public String toString() {
            return "NycOpenData{token=" + (tieneToken() ? "***" : "none") + "}";
        }
    }
}
