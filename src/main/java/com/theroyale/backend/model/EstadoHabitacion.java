package com.theroyale.backend.model;

// ===== Estado operativo de una habitación física, independiente de las fechas reservadas =====
// OCUPADA la pone el check-in y DISPONIBLE el check-out; MANTENIMIENTO y DESHABILITADA
// la sacan de la búsqueda de disponibilidad sin importar las fechas.
public enum EstadoHabitacion {
    DISPONIBLE, OCUPADA, MANTENIMIENTO, DESHABILITADA
}