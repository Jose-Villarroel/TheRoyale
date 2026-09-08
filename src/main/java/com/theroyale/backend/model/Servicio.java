package com.theroyale.backend.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "servicio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Servicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(nullable = false, length = 1000)
    private String descripcion;

    @Column(nullable = false)
    private double precio;

    @Column(nullable = false, length = 255)
    private String imagenUrl;

    @ElementCollection
    @CollectionTable(name = "servicio_caracteristica", joinColumns = @JoinColumn(name = "servicio_id"))
    @OrderColumn(name = "orden")
    @Column(name = "caracteristica", nullable = false, length = 500)
    @Builder.Default
    private List<String> caracteristicas = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "servicio_galeria_url", joinColumns = @JoinColumn(name = "servicio_id"))
    @OrderColumn(name = "orden")
    @Column(name = "galeria_url", nullable = false, length = 255)
    @Builder.Default
    private List<String> galeriaUrls = new ArrayList<>();
}
