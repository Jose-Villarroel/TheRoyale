package com.theroyale.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// ===== Modelo: una imagen de la galería de un tipo de habitación =====
@Entity
@Table(name = "imagen")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Imagen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_habitacion_id", nullable = false)
    @ToString.Exclude
    private TipoHabitacion tipoHabitacion;

    @Column(nullable = false, length = 255)
    private String url;

    private Integer orden;

    @Override
    public String toString() {
        return "Imagen{id=" + id + ", url='" + url + "', orden=" + orden + "}";
    }
}