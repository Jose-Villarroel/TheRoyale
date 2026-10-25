package com.theroyale.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.util.List;
import java.time.LocalDate;
import java.util.ArrayList;

@Entity
@Table(name = "cliente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellido;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, length = 255)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Column(nullable = false, unique = true, length = 20)
    private String cedula;

    @Column(length = 20)
    private String telefono;

    private LocalDate fechaRegistro;

    @JsonIgnore
    @OneToMany(mappedBy = "cliente")
    @Builder.Default
    @ToString.Exclude
    private List<Reserva> reservas = new ArrayList<>();

    public String getNombreCompleto() {
        return (nombre + " " + apellido).trim();
    }

    @Override
    public String toString() {
        return "Cliente{id=" + id + ", nombre='" + nombre + "', apellido='" + apellido +
                "', email='" + email + "', cedula='" + cedula + "', telefono='" + telefono +
                "', fechaRegistro=" + fechaRegistro + "}";
    }
}
