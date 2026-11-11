package com.theroyale.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// ===== Modelo: la reseña que un huésped deja sobre su estadía (1 por reserva FINALIZADA) =====
// Sin "ñ" en la clase y la tabla para evitar problemas de codificación; en la interfaz es "Review".
// cliente y tipoHabitacion se copian de la reserva al crearla: los listados públicos por tipo y el
// promedio se consultan sin hacer join con reserva y habitación.
@Entity
@Table(name = "resena")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resena {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // unique = true: una sola reseña por reserva (RN-3), igual que la cuenta
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reserva_id", nullable = false, unique = true)
    private Reserva reserva;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tipo_habitacion_id", nullable = false)
    private TipoHabitacion tipoHabitacion;

    @Column(nullable = false)
    private Integer calificacion;

    @Column(length = 100)
    private String titulo;

    @Column(nullable = false, length = 1000)
    private String comentario;

    // false = oculta por el admin: no sale en los listados públicos ni cuenta para el promedio (RN-8)
    @Column(nullable = false)
    @Builder.Default
    private boolean visible = true;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaModificacion;

    @Override
    public String toString() {
        return "Resena{id=" + id + ", calificacion=" + calificacion + ", titulo='" + titulo +
                "', visible=" + visible + ", fechaCreacion=" + fechaCreacion +
                ", fechaModificacion=" + fechaModificacion + "}";
    }
}
