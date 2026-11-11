package com.theroyale.backend.model;

// ===== De dónde sale el clima de un día del plan =====
// NWS para los próximos 7 días, Open-Meteo para los días 8 a 16 y clima típico para fechas más lejanas.
public enum TipoClima {
    PRONOSTICO_NWS, PRONOSTICO_OPEN_METEO, CLIMA_TIPICO
}
