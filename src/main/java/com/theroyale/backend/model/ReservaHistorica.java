package com.theroyale.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

// ===== Modelo: reserva del historial simulado que alimenta el pronóstico de ocupación =====
// Sale de src/main/resources/modelo/reservas-historicas.csv (ver Docs/modelo/README.md). Vive aparte
// de Reserva a propósito: no tiene cliente ni cuenta, y así no aparece en los listados de reservas.
// cancelada = true: la reserva se hizo pero no llegó a ocupar la habitación.
@Entity
@Table(name = "reserva_historica", indexes = @Index(name = "idx_historica_llegada", columnList = "llegada"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservaHistorica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate fechaCreacion;

    @Column(nullable = false)
    private LocalDate llegada;

    @Column(nullable = false)
    private Integer noches;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tipo_habitacion_id", nullable = false)
    private TipoHabitacion tipoHabitacion;

    @Column(nullable = false)
    private boolean cancelada;

    // Primera noche que ya no ocupa (el día de salida)
    public LocalDate getSalida() {
        return llegada.plusDays(noches);
    }
}
