package com.theroyale.backend.repository;

import com.theroyale.backend.model.EstadoReserva;
import com.theroyale.backend.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
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

    // ========================================================================================
    // ===== Consultas de agregación del dashboard de dirección =====
    // Todas suman dentro de la base de datos: ninguna trae reservas para recorrerlas en Java.
    //
    // Las cuatro que siguen son nativas de H2 y no JPQL. Motivo: una noche-habitación vendida es
    // cada noche del rango [fechaInicio, fechaFin) recortado al período consultado, y JPQL no tiene
    // aritmética de fechas con la que expresar ese recorte ni sumarlo. DATEDIFF, GREATEST y LEAST
    // lo resuelven en una sola pasada del motor. El proyecto solo usa H2 (en memoria al ejecutar y
    // en las pruebas), así que no hay otro dialecto que atender; si algún día se cambia de motor,
    // estas cuatro consultas son las únicas que habría que reescribir.
    //
    // Los estados van escritos dentro del SQL en lugar de parametrizados porque la definición de
    // "noche vendida" es una regla de negocio fija, no un filtro de la pantalla: CONFIRMADA,
    // EN_CURSO y FINALIZADA se venden; PENDIENTE (sin confirmar) y CANCELADA no.
    // ========================================================================================

    // Noches vendidas e ingresos de habitación del período. Columnas: noches, ingresos.
    // hastaExclusivo es el día siguiente al último del período: una reserva que termina ese día no
    // vendió esa noche, porque el huésped ya se fue.
    @Query(value = """
            SELECT COALESCE(SUM(DATEDIFF('DAY', GREATEST(r.fecha_inicio, :desde),
                                                LEAST(r.fecha_fin, :hastaExclusivo))), 0) AS noches,
                   COALESCE(SUM(DATEDIFF('DAY', GREATEST(r.fecha_inicio, :desde),
                                                LEAST(r.fecha_fin, :hastaExclusivo))
                                * r.precio_noche_acordado), 0) AS ingresos
            FROM reserva r
            WHERE r.estado IN ('CONFIRMADA','EN_CURSO','FINALIZADA')
              AND r.fecha_inicio < :hastaExclusivo
              AND r.fecha_fin > :desde
            """, nativeQuery = true)
    List<Object[]> nochesEIngresosHabitacion(@Param("desde") LocalDate desde,
                                             @Param("hastaExclusivo") LocalDate hastaExclusivo);

    // Serie diaria de noches vendidas e ingresos de habitación. Columnas: dia, noches, ingresos.
    // SYSTEM_RANGE genera el calendario del período para que los días sin ninguna reserva salgan en
    // cero en lugar de faltar: una gráfica de ocupación con días ausentes miente sobre la tendencia.
    // Cada noche imputa el precio acordado de su reserva al día que ocupa.
    @Query(value = """
            SELECT d.dia AS dia, COUNT(r.id) AS noches,
                   COALESCE(SUM(r.precio_noche_acordado), 0) AS ingresos
            FROM (SELECT DATEADD('DAY', x, CAST(:desde AS DATE)) AS dia FROM SYSTEM_RANGE(0, :dias)) d
            LEFT JOIN reserva r
                   ON r.fecha_inicio <= d.dia
                  AND r.fecha_fin > d.dia
                  AND r.estado IN ('CONFIRMADA','EN_CURSO','FINALIZADA')
            GROUP BY d.dia
            ORDER BY d.dia
            """, nativeQuery = true)
    List<Object[]> serieDiariaHabitaciones(@Param("desde") LocalDate desde,
                                           @Param("dias") int dias);

    // Lead time, ALOS y reservas atendidas. Columnas: leadTime (días), alos (noches), reservas.
    // El CAST a DOUBLE evita que AVG trunque a entero y nos haga reportar 7 días donde hay 7,4.
    @Query(value = """
            SELECT AVG(CAST(DATEDIFF('DAY', CAST(r.fecha_creacion AS DATE), r.fecha_inicio) AS DOUBLE)) AS leadTime,
                   AVG(CAST(DATEDIFF('DAY', r.fecha_inicio, r.fecha_fin) AS DOUBLE)) AS alos,
                   COUNT(r.id) AS reservas
            FROM reserva r
            WHERE r.estado IN ('CONFIRMADA','EN_CURSO','FINALIZADA')
              AND r.fecha_inicio < :hastaExclusivo
              AND r.fecha_fin > :desde
            """, nativeQuery = true)
    List<Object[]> promediosDeReserva(@Param("desde") LocalDate desde,
                                      @Param("hastaExclusivo") LocalDate hastaExclusivo);

    // Desglose por tipo de habitación. Columnas: tipoId, nombre, noches, ingresos.
    @Query(value = """
            SELECT th.id AS tipoId, th.nombre AS nombre,
                   COALESCE(SUM(DATEDIFF('DAY', GREATEST(r.fecha_inicio, :desde),
                                                LEAST(r.fecha_fin, :hastaExclusivo))), 0) AS noches,
                   COALESCE(SUM(DATEDIFF('DAY', GREATEST(r.fecha_inicio, :desde),
                                                LEAST(r.fecha_fin, :hastaExclusivo))
                                * r.precio_noche_acordado), 0) AS ingresos
            FROM reserva r
            JOIN habitacion h ON h.id = r.habitacion_id
            JOIN tipo_habitacion th ON th.id = h.tipo_habitacion_id
            WHERE r.estado IN ('CONFIRMADA','EN_CURSO','FINALIZADA')
              AND r.fecha_inicio < :hastaExclusivo
              AND r.fecha_fin > :desde
            GROUP BY th.id, th.nombre
            """, nativeQuery = true)
    List<Object[]> desglosePorTipoHabitacion(@Param("desde") LocalDate desde,
                                             @Param("hastaExclusivo") LocalDate hastaExclusivo);

    // ===== Operación del día (en JPQL: aquí basta con contar filas) =====

    @Query("SELECT COUNT(r) FROM Reserva r WHERE r.fechaInicio = :fecha AND r.estado IN :estados")
    long contarLlegadas(@Param("fecha") LocalDate fecha,
                        @Param("estados") Collection<EstadoReserva> estados);

    @Query("SELECT COUNT(r) FROM Reserva r WHERE r.fechaFin = :fecha AND r.estado IN :estados")
    long contarSalidas(@Param("fecha") LocalDate fecha,
                       @Param("estados") Collection<EstadoReserva> estados);

    // Huéspedes en casa: ya hicieron check-in y todavía no les toca salir
    @Query("""
            SELECT COUNT(r) FROM Reserva r
            WHERE r.estado = :estado
              AND r.fechaInicio <= :fecha
              AND r.fechaFin > :fecha
            """)
    long contarEnCasa(@Param("estado") EstadoReserva estado, @Param("fecha") LocalDate fecha);

    // ===== Tasa de cancelación: sobre las reservas CREADAS en el período, no las que llegaban =====
    // Se mide por fechaCreacion porque la pregunta es "de lo que se reservó en estos días, cuánto se
    // cayó"; usar fechaInicio mezclaría cancelaciones de reservas vendidas meses antes.
    long countByFechaCreacionBetween(LocalDateTime desde, LocalDateTime hasta);

    long countByEstadoAndFechaCreacionBetween(EstadoReserva estado, LocalDateTime desde, LocalDateTime hasta);

    // ===== Reservas PENDIENTE que llegan pronto y que nadie ha confirmado (alerta) =====
    @Query("""
            SELECT r FROM Reserva r
            WHERE r.estado = :estado
              AND r.fechaInicio BETWEEN :desde AND :hasta
            ORDER BY r.fechaInicio
            """)
    List<Reserva> buscarPorConfirmarEntre(@Param("estado") EstadoReserva estado,
                                          @Param("desde") LocalDate desde,
                                          @Param("hasta") LocalDate hasta);

    // ===== Huéspedes recurrentes: reservas del período de clientes que ya se habían alojado =====
    // "Antes" es una reserva vendible con fechaInicio anterior a la actual. Los nuevos se obtienen
    // restando, para no repetir la misma consulta con la condición invertida.
    @Query("""
            SELECT COUNT(r) FROM Reserva r
            WHERE r.estado IN :estados
              AND r.fechaInicio < :hastaExclusivo
              AND r.fechaFin > :desde
              AND EXISTS (SELECT 1 FROM Reserva previa
                          WHERE previa.cliente = r.cliente
                            AND previa.estado IN :estados
                            AND previa.fechaInicio < r.fechaInicio)
            """)
    long contarReservasDeClientesRecurrentes(@Param("estados") Collection<EstadoReserva> estados,
                                             @Param("desde") LocalDate desde,
                                             @Param("hastaExclusivo") LocalDate hastaExclusivo);
}
