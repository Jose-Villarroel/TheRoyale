package com.theroyale.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// ===== Modelo: precio que el sistema propone para un tipo de habitación en una semana =====
// Lo genera RecomendacionPrecioService (cada lunes o a pedido). No cambia ningún precio por sí solo:
// el admin lo aplica (se crea una TarifaAplicada) o lo descarta.
@Entity
@Table(name = "recomendacion_precio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecomendacionPrecio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tipo_habitacion_id", nullable = false)
    private TipoHabitacion tipoHabitacion;

    // Lunes de la semana recomendada (cubre de lunes a domingo)
    @Column(nullable = false)
    private LocalDate semanaInicio;

    @Column(nullable = false)
    private double factorTemporada;

    // Ocupación esperada de la semana (0 a 1): la mayor entre lo reservado y lo pronosticado
    @Column(nullable = false)
    private double ocupacionEsperada;

    @Column(nullable = false)
    private double factorDemanda;

    @Column(nullable = false)
    private double factorRecomendado;

    // Factor vigente al generarla (1 = precio base)
    @Column(nullable = false)
    private double factorActual;

    // Precio base de la habitación más barata del tipo: la referencia que ve el admin
    @Column(nullable = false)
    private BigDecimal precioBaseReferencia;

    @Column(nullable = false)
    private BigDecimal precioActual;

    @Column(nullable = false)
    private BigDecimal precioRecomendado;

    @Column(nullable = false, length = 200)
    private String motivo;

    // Etiqueta que verá el huésped si se aplica ("High season", "High demand", "Special rate")
    @Column(length = 40)
    private String etiqueta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoRecomendacion estado;

    @Column(nullable = false)
    private LocalDateTime fechaGeneracion;

    private LocalDateTime fechaDecision;

    @Column(length = 120)
    private String decididaPor;

    public LocalDate getSemanaFin() {
        return semanaInicio.plusDays(6);
    }
}
