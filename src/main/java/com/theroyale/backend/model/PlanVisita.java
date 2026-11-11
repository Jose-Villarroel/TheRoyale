package com.theroyale.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;

// ===== Modelo: plan de visita vigente de una reserva (uno por reserva) =====
// Preferencias y contenido se guardan como JSON: el plan se lee y se reemplaza entero o por día,
// nunca se consulta bloque por bloque. El contenido ya está validado y enriquecido con las fuentes.
@Entity
@Table(name = "plan_visita")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanVisita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserva_id", nullable = false, unique = true)
    @ToString.Exclude
    private Reserva reserva;

    // JSON de PreferenciasPlan
    @Lob
    @Column(nullable = false)
    private String preferencias;

    // JSON de ContenidoPlan: resumen, "entendido", días con clima y bloques, días en revisión
    @Lob
    @Column(nullable = false)
    private String contenido;

    // Lo que el huésped escribió en texto libre (null si usó el modo guiado)
    @Column(length = 1000)
    private String descripcion;

    // Ej.: "OPEN_METEO,NYC_OPEN_DATA"
    @Column(length = 100)
    private String fuentesUsadas;

    @Column(length = 100)
    private String fuentesNoDisponibles;

    // true si el clima cambió y un bloque al aire libre cae en un día de lluvia
    @Column(nullable = false)
    private boolean requiereRevision;

    // Modelo de Gemini que respondió (principal o de respaldo)
    @Column(length = 60)
    private String modeloIa;

    @Column(nullable = false)
    private LocalDateTime fechaGeneracion;

    @Column(nullable = false)
    private LocalDateTime fechaActualizacion;

    // Límite de generaciones por reserva y por día
    @Column(nullable = false)
    private int generacionesHoy;

    private LocalDate fechaUltimaGeneracion;
}
