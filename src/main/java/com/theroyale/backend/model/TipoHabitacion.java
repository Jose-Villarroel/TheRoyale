package com.theroyale.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

// ===== Modelo: representa un tipo de habitación (Normal, Executive, VIP, Luxury, Presidential Suite) =====
@Entity
@Table(name = "tipo_habitacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoHabitacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(length = 255)
    private String imagenUrl;

    // ===== Lado inverso de la relación: un tipo tiene muchas habitaciones =====
    // mappedBy = "tipoHabitacion" apunta al nombre EXACTO del campo en la clase Habitacion,
    // no al nombre de la columna. Este lado no es "dueño" de la relación, por eso no lleva @JoinColumn.
    @OneToMany(mappedBy = "tipoHabitacion", orphanRemoval = false)
    @Builder.Default
    @ToString.Exclude
    private List<Habitacion> habitaciones = new ArrayList<>();

    @Override
    public String toString() {
        return "TipoHabitacion{id=" + id + ", nombre='" + nombre + "', descripcion='" + descripcion +
                "', imagenUrl='" + imagenUrl + "'}";
    }
}
