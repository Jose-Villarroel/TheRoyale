package com.theroyale.backend.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

// ===== Modelo: plato de la carta de uno de los restaurantes del hotel (editable por el admin) =====
// Las etiquetas (vegano, sin gluten...) son una colección simple de enums, sin entidad propia.
@Entity
@Table(name = "plato_menu")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatoMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RestauranteHotel restaurante;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoriaPlato categoria;

    // EAGER: el chat arma tarjetas y el prompt fuera de una petición web (sin sesión abierta)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "plato_menu_etiqueta", joinColumns = @JoinColumn(name = "plato_menu_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "etiqueta", nullable = false, length = 20)
    @Builder.Default
    private Set<EtiquetaPlato> etiquetas = new LinkedHashSet<>();

    @Column(nullable = false)
    private boolean disponible;

    @Column(length = 255)
    private String imagenUrl;
}
