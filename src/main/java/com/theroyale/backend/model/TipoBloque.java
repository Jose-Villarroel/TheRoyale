package com.theroyale.backend.model;

// ===== Tipo de cada bloque del plan de visita: de qué fuente sale =====
// LUGAR (catálogo LugarNyc), EVENTO (NYC Open Data o Ticketmaster), SERVICIO_HOTEL (Servicio + característica),
// RESTAURANTE_HOTEL (RestauranteHotel) y TIEMPO_LIBRE (nota sin fuente).
public enum TipoBloque {
    LUGAR, EVENTO, SERVICIO_HOTEL, RESTAURANTE_HOTEL, TIEMPO_LIBRE
}
