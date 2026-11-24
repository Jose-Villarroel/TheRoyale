package com.theroyale.backend.repository;

import com.theroyale.backend.model.EstadoCuenta;
import com.theroyale.backend.model.Pago;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// ===== Acceso a datos de pagos =====
// Los pagos se crean desde Cuenta (cascade); aquí solo se consulta si un operador tiene pagos registrados.
@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

    boolean existsByOperadorId(Long operadorId);

    // ===== Dashboard: caja cobrada en el periodo =====
    // Se imputa por la fecha del pago. "Cobrado" no es "facturado": el dashboard los muestra
    // siempre por separado, porque un hotel puede facturar mucho y haber cobrado poco.
    @Query("""
            SELECT COALESCE(SUM(p.monto), 0)
            FROM Pago p
            WHERE p.fecha >= :desde AND p.fecha < :hasta
            """)
    BigDecimal sumarCobrado(@Param("desde") LocalDateTime desde,
                            @Param("hasta") LocalDateTime hasta);

    // Total pagado en las cuentas que siguen abiertas: segunda mitad del saldo pendiente
    @Query("""
            SELECT COALESCE(SUM(p.monto), 0)
            FROM Pago p
            WHERE p.cuenta.estado = :estadoCuenta
            """)
    BigDecimal sumarPagosDeCuentas(@Param("estadoCuenta") EstadoCuenta estadoCuenta);

    // Desglose por metodo de pago. Columnas: metodo, importe, numero de pagos.
    // metodoPago es un String libre en el modelo (CARD, CASH, TRANSFER), no un enum: se agrupa tal cual.
    @Query("""
            SELECT p.metodoPago, SUM(p.monto), COUNT(p)
            FROM Pago p
            WHERE p.fecha >= :desde AND p.fecha < :hasta
            GROUP BY p.metodoPago
            """)
    List<Object[]> desglosePorMetodoPago(@Param("desde") LocalDateTime desde,
                                         @Param("hasta") LocalDateTime hasta);

    // Desempeno de operadores en cobros. Columnas: nombre, importe, numero de pagos.
    // Pago.operador es opcional: los pagos sin operador quedan fuera del desglose a proposito,
    // porque este bloque mide el trabajo del personal, no la caja total (esa es sumarCobrado).
    @Query("""
            SELECT p.operador.nombre, SUM(p.monto), COUNT(p)
            FROM Pago p
            WHERE p.fecha >= :desde AND p.fecha < :hasta
              AND p.operador IS NOT NULL
            GROUP BY p.operador.nombre
            """)
    List<Object[]> pagosPorOperador(@Param("desde") LocalDateTime desde,
                                    @Param("hasta") LocalDateTime hasta);

    // Serie diaria de caja. Columnas: dia, importe. Nativa por el mismo motivo que la de consumos.
    @Query(value = """
            SELECT CAST(p.fecha AS DATE) AS dia, SUM(p.monto) AS total
            FROM pago p
            WHERE p.fecha >= :desde AND p.fecha < :hasta
            GROUP BY CAST(p.fecha AS DATE)
            ORDER BY dia
            """, nativeQuery = true)
    List<Object[]> serieDiariaCobrado(@Param("desde") LocalDateTime desde,
                                      @Param("hasta") LocalDateTime hasta);
}
