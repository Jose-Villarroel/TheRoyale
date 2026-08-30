package com.theroyale.backend.model;

// ===== Modelo: representa un tipo de habitación (Normal, Executive, VIP, Luxury) =====
public class TipoHabitacion {

    private Long id;
    private String nombre;
    private String descripcion;

    public TipoHabitacion() {
    }

    public TipoHabitacion(Long id, String nombre, String descripcion) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}