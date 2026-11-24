package com.theroyale.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

// ===== Modelo: precio aprobado por el admin para un tipo de habitación en un rango de fechas =====
// Es lo único que cambia el precio real: PrecioService multiplica el precio base de cada habitación
// del tipo por este factor en las noches cubiertas. Las reservas ya hechas no cambian (guardan su
// precioNocheAcordado).
@Entity
@Table(name = "tarifa_aplicada")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TarifaAplicada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tipo_habitacion_id", nullable = false)
    private TipoHabitacion tipoHabitacion;

    @Column(nullable = false)
    private LocalDate fechaInicio;

    // Incluida
    @Column(nullable = false)
    private LocalDate fechaFin;

    @Column(nullable = false)
    private double factor;

    @Column(length = 40)
    private String etiqueta;

    @Column(nullable = false, length = 120)
    private String aprobadaPor;

    @Column(nullable = false)
    private LocalDateTime fechaAprobacion;

    // Recomendación que la originó (null si se creó de otra forma)
    private Long recomendacionId;

    public boolean cubre(LocalDate noche) {
        return !noche.isBefore(fechaInicio) && !noche.isAfter(fechaFin);
    }
}
