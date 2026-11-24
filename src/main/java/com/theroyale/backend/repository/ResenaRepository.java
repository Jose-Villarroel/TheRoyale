package com.theroyale.backend.repository;

import com.theroyale.backend.model.Resena;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// ===== Acceso a datos de reseñas: listados por tipo y por cliente, y los agregados del resumen =====
// Las consultas públicas (listado por tipo y resúmenes) filtran visible = true (RN-8).
@Repository
public interface ResenaRepository extends JpaRepository<Resena, Long> {

    // Página pública del tipo: solo las visibles, de la más reciente a la más antigua
    List<Resena> findByTipoHabitacionIdAndVisibleTrueOrderByFechaCreacionDesc(Long tipoId);

    // "My reservations": las del cliente, incluidas las ocultas
    List<Resena> findByClienteIdOrderByFechaCreacionDesc(Long clienteId);

    Optional<Resena> findByReservaId(Long reservaId);

    boolean existsByReservaId(Long reservaId);

    // Moderación del admin: todas (visibles y ocultas); un filtro nulo no restringe
    @Query("""
            SELECT r FROM Resena r
            WHERE (:tipoId IS NULL OR r.tipoHabitacion.id = :tipoId)
              AND (:calificacion IS NULL OR r.calificacion = :calificacion)
            ORDER BY r.fechaCreacion DESC
            """)
    List<Resena> buscarParaModeracion(@Param("tipoId") Long tipoId,
                                      @Param("calificacion") Integer calificacion);

    // Resumen de un tipo: filas [calificacion, cantidad] para armar promedio y distribución
    @Query("select r.calificacion, count(r) from Resena r " +
           "where r.tipoHabitacion.id = :tipoId and r.visible = true group by r.calificacion")
    List<Object[]> contarPorCalificacion(@Param("tipoId") Long tipoId);

    // Promedio y total de todos los tipos de una vez (para /rooms): filas [tipoId, promedio, cantidad]
    @Query("select r.tipoHabitacion.id, avg(r.calificacion), count(r) from Resena r " +
           "where r.visible = true group by r.tipoHabitacion.id")
    List<Object[]> resumenPorTipo();

    // Complemento de resumenPorTipo: la distribución por estrellas de todos los tipos en una sola
    // consulta, filas [tipoId, calificacion, cantidad]. Evita una consulta por tipo en /rooms.
    @Query("select r.tipoHabitacion.id, r.calificacion, count(r) from Resena r " +
           "where r.visible = true group by r.tipoHabitacion.id, r.calificacion")
    List<Object[]> contarPorTipoYCalificacion();

    // ========================================================================================
    // ===== Agregados del dashboard de dirección (todos sobre reseñas visibles, RN-8) =====
    // Una reseña oculta por moderación no debe mover el rating que ve el administrador: si la
    // escondimos del público, tampoco cuenta como reputación.
    // ========================================================================================

    // Rating global del hotel. Columnas: promedio, total
    @Query("SELECT COALESCE(AVG(r.calificacion), 0), COUNT(r) FROM Resena r WHERE r.visible = true")
    List<Object[]> resumenGlobal();

    // Rating de las reseñas escritas dentro de un período, para poder comparar con el anterior.
    // Columnas: promedio, total
    @Query("""
            SELECT COALESCE(AVG(r.calificacion), 0), COUNT(r)
            FROM Resena r
            WHERE r.visible = true
              AND r.fechaCreacion >= :desde AND r.fechaCreacion < :hasta
            """)
    List<Object[]> resumenEnPeriodo(@Param("desde") LocalDateTime desde,
                                    @Param("hasta") LocalDateTime hasta);

    // Distribución de estrellas de todo el hotel. Columnas: calificación, conteo.
    // Las estrellas que nadie puso no salen: el servicio rellena con cero las cinco posiciones.
    @Query("""
            SELECT r.calificacion, COUNT(r)
            FROM Resena r
            WHERE r.visible = true
            GROUP BY r.calificacion
            """)
    List<Object[]> distribucionGlobal();

    // Rating por tipo de habitación, con el nombre resuelto en la misma consulta para no pedir
    // después los tipos uno por uno. Columnas: tipoId, nombre, promedio, total
    @Query("""
            SELECT r.tipoHabitacion.id, r.tipoHabitacion.nombre, AVG(r.calificacion), COUNT(r)
            FROM Resena r
            WHERE r.visible = true
            GROUP BY r.tipoHabitacion.id, r.tipoHabitacion.nombre
            """)
    List<Object[]> ratingPorTipoConNombre();

    // Últimas reseñas visibles; el límite lo pone el servicio con un Pageable
    @Query("""
            SELECT r FROM Resena r
            WHERE r.visible = true
            ORDER BY r.fechaCreacion DESC
            """)
    List<Resena> buscarRecientes(Pageable pagina);

    // Reseñas malas recientes (alerta): una queja de 1 o 2 estrellas se responde en días, no en meses
    @Query("""
            SELECT r FROM Resena r
            WHERE r.visible = true
              AND r.calificacion <= :calificacionMaxima
              AND r.fechaCreacion >= :desde
            ORDER BY r.fechaCreacion DESC
            """)
    List<Resena> buscarNegativasDesde(@Param("calificacionMaxima") int calificacionMaxima,
                                      @Param("desde") LocalDateTime desde);
}
