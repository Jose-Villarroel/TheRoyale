package com.theroyale.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

// ===== Modelo: temporada de precios que define el admin (ej. "Holiday season", x1.35) =====
// Solo alimenta las recomendaciones de precio: el precio real cambia cuando el admin aplica una.
@Entity
@Table(name = "temporada")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Temporada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false)
    private LocalDate fechaInicio;

    // Incluida: la temporada cubre la noche de fechaFin
    @Column(nullable = false)
    private LocalDate fechaFin;

    // Multiplicador sobre el precio base, entre 0.5 y 2.0
    @Column(nullable = false)
    private double factor;

    public boolean cubre(LocalDate noche) {
        return !noche.isBefore(fechaInicio) && !noche.isAfter(fechaFin);
    }
}
