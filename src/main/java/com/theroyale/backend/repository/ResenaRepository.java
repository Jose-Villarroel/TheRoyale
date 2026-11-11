package com.theroyale.backend.repository;

import com.theroyale.backend.model.Resena;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
