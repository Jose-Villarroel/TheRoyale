package com.theroyale.backend.model;

import java.util.List;

public class Servicio {

    private Long id;
    private String nombre;
    private String descripcion;
    private double precio;
    private String imagenUrl;
    private List<String> caracteristicas;
    private List<String> galeriaUrls;

    public Servicio() {}

    public Servicio(Long id, String nombre, String descripcion, double precio, String imagenUrl) {
        this.id = id; this.nombre = nombre; this.descripcion = descripcion;
        this.precio = precio; this.imagenUrl = imagenUrl;
    }

    public Servicio(Long id, String nombre, String descripcion, double precio, String imagenUrl, List<String> caracteristicas, List<String> galeriaUrls) {
        this(id, nombre, descripcion, precio, imagenUrl);
        this.caracteristicas = caracteristicas;
        this.galeriaUrls = galeriaUrls;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public double getPrecio() { return precio; }
    public void setPrecio(double precio) { this.precio = precio; }
    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }
    public List<String> getCaracteristicas() { return caracteristicas; }
    public void setCaracteristicas(List<String> caracteristicas) { this.caracteristicas = caracteristicas; }
    public List<String> getGaleriaUrls() { return galeriaUrls; }
    public void setGaleriaUrls(List<String> galeriaUrls) { this.galeriaUrls = galeriaUrls; }
}