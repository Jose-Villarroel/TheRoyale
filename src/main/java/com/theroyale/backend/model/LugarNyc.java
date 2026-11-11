package com.theroyale.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// ===== Modelo: lugar emblemático de Nueva York aprobado por el hotel (catálogo curado) =====
// El plan de visita solo puede sugerir lugares de este catálogo: la IA elige entre ellos por su id
// y el nombre, la URL y el tiempo de traslado salen de aquí, no de lo que escriba la IA.
@Entity
@Table(name = "lugar_nyc")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LugarNyc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoriaLugar categoria;

    @Column(nullable = false)
    private double latitud;

    @Column(nullable = false)
    private double longitud;

    @Column(nullable = false)
    private boolean bajoTecho;

    @Column(nullable = false)
    private int duracionMinutos;

    // Texto de referencia para la IA y el huésped, p. ej. "Daily 10:00-17:00"
    @Column(length = 200)
    private String horarioReferencia;

    @Column(nullable = false, length = 255)
    private String urlOficial;

    @Column(nullable = false)
    private boolean activo;
}
