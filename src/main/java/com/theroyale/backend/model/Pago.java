package com.theroyale.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// ===== Modelo: un pago realizado sobre la cuenta de una reserva =====
@Entity
@Table(name = "pago")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_id", nullable = false)
    @ToString.Exclude
    private Cuenta cuenta;

    // Nullable: por si el pago se procesa sin intervención directa de un operador
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operador_id", nullable = true)
    @ToString.Exclude
    private Operador operador;

    @Column(nullable = false)
    private BigDecimal monto;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false, length = 30)
    private String metodoPago;

    @Override
    public String toString() {
        return "Pago{id=" + id + ", monto=" + monto + ", fecha=" + fecha + ", metodoPago='" + metodoPago + "'}";
    }
}