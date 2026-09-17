package com.theroyale.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

// ===== Modelo: administrador del sistema, con permisos de gestión sobre otras entidades =====
@Entity
@Table(name = "admin")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    // ===== Relaciones muchos a muchos: qué administra cada admin =====
    // Cada una crea su propia tabla intermedia (join table) en la base de datos.

    @ManyToMany
    @JoinTable(
            name = "admin_tipo_habitacion",
            joinColumns = @JoinColumn(name = "admin_id"),
            inverseJoinColumns = @JoinColumn(name = "tipo_habitacion_id")
    )
    @Builder.Default
    @ToString.Exclude
    private List<TipoHabitacion> tiposHabitacionAdministrados = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "admin_habitacion",
            joinColumns = @JoinColumn(name = "admin_id"),
            inverseJoinColumns = @JoinColumn(name = "habitacion_id")
    )
    @Builder.Default
    @ToString.Exclude
    private List<Habitacion> habitacionesAdministradas = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "admin_servicio",
            joinColumns = @JoinColumn(name = "admin_id"),
            inverseJoinColumns = @JoinColumn(name = "servicio_id")
    )
    @Builder.Default
    @ToString.Exclude
    private List<Servicio> serviciosAdministrados = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "admin_operador",
            joinColumns = @JoinColumn(name = "admin_id"),
            inverseJoinColumns = @JoinColumn(name = "operador_id")
    )
    @Builder.Default
    @ToString.Exclude
    private List<Operador> operadoresAdministrados = new ArrayList<>();

    @Override
    public String toString() {
        return "Admin{id=" + id + ", nombre='" + nombre + "', email='" + email + "'}";
    }
}