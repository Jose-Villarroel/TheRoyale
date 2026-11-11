package com.theroyale.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// ===== Ubicación del hotel (propiedades hotel.* de application.properties) =====
// La dirección es ficticia (The Royale no existe); las coordenadas se usan para calcular distancias
// a los lugares y eventos del plan de visita y para pedir el clima de esa esquina de Manhattan.
@Component
@ConfigurationProperties(prefix = "hotel")
@Getter
@Setter
public class HotelProperties {

    private String direccion = "640 Fifth Avenue, New York, NY 10019";

    private double latitud = 40.7590;

    private double longitud = -73.9772;
}
