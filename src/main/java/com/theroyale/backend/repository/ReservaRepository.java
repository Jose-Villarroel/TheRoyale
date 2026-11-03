package com.theroyale.backend.repository;

import com.theroyale.backend.model.EstadoReserva;
import com.theroyale.backend.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// ===== Acceso a datos de reservas: listados por estado, por cliente y llegadas/salidas del día =====
@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    boolean existsByClienteId(Long clienteId);

    boolean existsByHabitacionId(Long habitacionId);

    List<Reserva> findAllByOrderByFechaInicioDesc();

    // Filtro por estado de los paneles de reservas, de la llegada más lejana a la más cercana
    @Query("""
            SELECT r FROM Reserva r
            WHERE r.estado = :estado
            ORDER BY r.fechaInicio DESC
            """)
    List<Reserva> findByEstadoOrderByFechaInicioDesc(@Param("estado") EstadoReserva estado);

    List<Reserva> findByClienteIdOrderByFechaInicioDesc(Long clienteId);

    // Portal del cliente: según los estados que reciba son sus reservas activas o su historial
    @Query("""
            SELECT r FROM Reserva r
            WHERE r.cliente.id = :clienteId
              AND r.estado IN :estados
            ORDER BY r.fechaInicio DESC
            """)
    List<Reserva> findByClienteIdAndEstadoInOrderByFechaInicioDesc(@Param("clienteId") Long clienteId,
                                                                   @Param("estados") List<EstadoReserva> estados);

    // Llegadas del día que ve el operador
    @Query("""
            SELECT r FROM Reserva r
            WHERE r.fechaInicio = :fechaInicio
              AND r.estado IN :estados
            """)
    List<Reserva> findByFechaInicioAndEstadoIn(@Param("fechaInicio") LocalDate fechaInicio,
                                               @Param("estados") List<EstadoReserva> estados);

    // Salidas del día que ve el operador
    @Query("""
            SELECT r FROM Reserva r
            WHERE r.fechaFin = :fechaFin
              AND r.estado = :estado
            """)
    List<Reserva> findByFechaFinAndEstado(@Param("fechaFin") LocalDate fechaFin,
                                          @Param("estado") EstadoReserva estado);

    long countByEstado(EstadoReserva estado);

    // Busca la reserva activa de una habitacion por su numero (para el portal del operador)
    Optional<Reserva> findFirstByHabitacionNumeroAndEstadoIn(String numero, List<EstadoReserva> estados);
}
