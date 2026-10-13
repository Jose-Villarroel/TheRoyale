package com.theroyale.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

// ===== Modelo: empleado del hotel que gestiona reservas, consumos y pagos =====
@Entity
@Table(name = "operador")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Operador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, length = 255)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoOperador tipo;

    // ===== Lados inversos: consumos y pagos que este operador ha registrado =====
    @JsonIgnore
    @OneToMany(mappedBy = "operador")
    @Builder.Default
    @ToString.Exclude
    private List<ItemConsumo> consumosRegistrados = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "operador")
    @Builder.Default
    @ToString.Exclude
    private List<Pago> pagosRegistrados = new ArrayList<>();

    @Override
    public String toString() {
        return "Operador{id=" + id + ", nombre='" + nombre + "', email='" + email + "', tipo=" + tipo + "}";
    }
}