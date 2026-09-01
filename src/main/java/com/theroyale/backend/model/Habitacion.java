package com.theroyale.backend.model;

// ===== Modelo: representa una habitación física, ligada a un tipo de habitación =====
public class Habitacion {

    private Long id;
    private String numero;
    private TipoHabitacion tipoHabitacion;
    private double precio;
    private String estado;

    public Habitacion() {
    }

    public Habitacion(Long id, String numero, TipoHabitacion tipoHabitacion, double precio, String estado) {
        this.id = id;
        this.numero = numero;
        this.tipoHabitacion = tipoHabitacion;
        this.precio = precio;
        this.estado = estado;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public TipoHabitacion getTipoHabitacion() {
        return tipoHabitacion;
    }

    public void setTipoHabitacion(TipoHabitacion tipoHabitacion) {
        this.tipoHabitacion = tipoHabitacion;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
