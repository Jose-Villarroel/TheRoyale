package com.theroyale.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @Override
    public String toString() {
        return "TipoHabitacion{id=" + id + ", nombre='" + nombre + "', descripcion='" + descripcion +
                "', imagenUrl='" + imagenUrl + "'}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TipoHabitacion t)) return false;
        return id != null && id.equals(t.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
