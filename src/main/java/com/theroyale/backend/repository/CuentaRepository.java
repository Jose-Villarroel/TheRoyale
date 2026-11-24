package com.theroyale.backend.repository;

import com.theroyale.backend.model.Cuenta;
import com.theroyale.backend.model.EstadoCuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// ===== Acceso a datos de cuentas de consumo =====
// Se buscan por reserva porque la relación es 1 a 1 y el resto del sistema solo conoce la reserva.
@Repository
public interface CuentaRepository extends JpaRepository<Cuenta, Long> {

    Optional<Cuenta> findByReservaId(Long reservaId);

    long countByEstado(EstadoCuenta estado);

    // ===== Dashboard: saldo de las cuentas abiertas cuya reserva termina en una fecha =====
    // El saldo se calcula con dos subconsultas de agregacion dentro de la misma consulta, para no
    // traer los consumos ni los pagos a memoria. Devuelve tambien el nombre del huesped y la
    // habitacion porque la alerta "sale hoy con saldo" los muestra sin pedir otra peticion.
    // Columnas: reservaId, numero de habitacion, nombre, apellido, saldo.
    @Query("""
            SELECT c.reserva.id, c.reserva.habitacion.numero,
                   c.reserva.cliente.nombre, c.reserva.cliente.apellido,
                   (SELECT COALESCE(SUM(i.cantidad * i.precioUnitario), 0) FROM ItemConsumo i WHERE i.cuenta = c)
                 - (SELECT COALESCE(SUM(p.monto), 0) FROM Pago p WHERE p.cuenta = c)
            FROM Cuenta c
            WHERE c.estado = :estado
              AND c.reserva.fechaFin = :fecha
            """)
    List<Object[]> saldosDeCuentasConSalidaEn(@Param("estado") EstadoCuenta estado,
                                              @Param("fecha") LocalDate fecha);
}
