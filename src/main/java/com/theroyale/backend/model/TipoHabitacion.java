package com.theroyale.backend.model;

// ===== Modelo: representa un tipo de habitación (Normal, Executive, VIP, Luxury) =====
public class TipoHabitacion {

    private Long id;
    private String nombre;
    private String descripcion;
    private String imagenUrl;

    public TipoHabitacion() {
    }

    public TipoHabitacion(Long id, String nombre, String descripcion, String imagenUrl) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.imagenUrl = imagenUrl;
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

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }
}
