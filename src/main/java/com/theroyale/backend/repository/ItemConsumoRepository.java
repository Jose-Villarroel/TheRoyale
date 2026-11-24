package com.theroyale.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.theroyale.backend.model.EstadoCuenta;
import com.theroyale.backend.model.ItemConsumo;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// ===== Acceso a datos de los consumos cargados a las cuentas =====
// Las consultas exists* permiten impedir el borrado de servicios u operadores con historial.
@Repository 
public interface ItemConsumoRepository extends JpaRepository<ItemConsumo, Long> {

    boolean existsByServicioId(Long servicioId);

    boolean existsByOperadorId(Long operadorId);

    // ===== Dashboard: ingresos por servicios del periodo =====
    // Se imputan por fechaHora del consumo (cuando se presto el servicio), no por la fecha de la
    // reserva ni por cuando se pago: es lo que el hotel facturo en esos dias.
    @Query("""
            SELECT COALESCE(SUM(i.cantidad * i.precioUnitario), 0)
            FROM ItemConsumo i
            WHERE i.fechaHora >= :desde AND i.fechaHora < :hasta
            """)
    BigDecimal sumarIngresosServicios(@Param("desde") LocalDateTime desde,
                                      @Param("hasta") LocalDateTime hasta);

    // Total consumido en las cuentas que siguen abiertas: primera mitad del saldo pendiente
    @Query("""
            SELECT COALESCE(SUM(i.cantidad * i.precioUnitario), 0)
            FROM ItemConsumo i
            WHERE i.cuenta.estado = :estadoCuenta
            """)
    BigDecimal sumarConsumosDeCuentas(@Param("estadoCuenta") EstadoCuenta estadoCuenta);

    // Desglose por servicio. Columnas: nombre, importe, unidades vendidas
    @Query("""
            SELECT i.servicio.nombre, SUM(i.cantidad * i.precioUnitario), SUM(i.cantidad)
            FROM ItemConsumo i
            WHERE i.fechaHora >= :desde AND i.fechaHora < :hasta
            GROUP BY i.servicio.nombre
            """)
    List<Object[]> desglosePorServicio(@Param("desde") LocalDateTime desde,
                                       @Param("hasta") LocalDateTime hasta);

    // Desempeno de operadores en consumos. Columnas: nombre, importe, numero de consumos
    @Query("""
            SELECT i.operador.nombre, SUM(i.cantidad * i.precioUnitario), COUNT(i)
            FROM ItemConsumo i
            WHERE i.fechaHora >= :desde AND i.fechaHora < :hasta
            GROUP BY i.operador.nombre
            """)
    List<Object[]> consumosPorOperador(@Param("desde") LocalDateTime desde,
                                       @Param("hasta") LocalDateTime hasta);

    // Serie diaria de ingresos por servicios. Columnas: dia, importe.
    // Nativa porque hay que truncar un TIMESTAMP a dia para agrupar, y la suma debe quedarse en la base.
    @Query(value = """
            SELECT CAST(i.fecha_hora AS DATE) AS dia, SUM(i.cantidad * i.precio_unitario) AS total
            FROM item_consumo i
            WHERE i.fecha_hora >= :desde AND i.fecha_hora < :hasta
            GROUP BY CAST(i.fecha_hora AS DATE)
            ORDER BY dia
            """, nativeQuery = true)
    List<Object[]> serieDiariaServicios(@Param("desde") LocalDateTime desde,
                                        @Param("hasta") LocalDateTime hasta);
}
