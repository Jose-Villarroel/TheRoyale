package com.theroyale.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

// ===== Modelo: una reserva de habitación hecha por un cliente =====
@Entity
@Table(name = "reserva")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "habitacion_id", nullable = false)
    private Habitacion habitacion;

    // Nullable: la reserva puede crearse sin que un operador la haya gestionado todavía
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operador_id", nullable = true)
    @ToString.Exclude
    private Operador operador;

    @Column(nullable = false)
    private LocalDate fechaInicio;

    @Column(nullable = false)
    private LocalDate fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoReserva estado;

    @Column(nullable = false)
    private Integer cantidadPersonas;

    @Column(nullable = false)
    private BigDecimal precioNocheAcordado;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    // ===== Lado inverso: la cuenta asociada a esta reserva (relación 1 a 1) =====
    @OneToOne(mappedBy = "reserva", cascade = CascadeType.ALL)
    @ToString.Exclude
    private Cuenta cuenta;

    @Override
    public String toString() {
        return "Reserva{id=" + id + ", fechaInicio=" + fechaInicio + ", fechaFin=" + fechaFin +
                ", estado=" + estado + ", cantidadPersonas=" + cantidadPersonas +
                ", precioNocheAcordado=" + precioNocheAcordado + "}";
    }
}