package com.theroyale.backend.model;

// ===== Estado de una recomendación de precio =====
// PENDIENTE espera la decisión del admin; VENCIDA = la semana empezó sin que nadie la aplicara.
public enum EstadoRecomendacion {
    PENDIENTE, APLICADA, DESCARTADA, VENCIDA
}
