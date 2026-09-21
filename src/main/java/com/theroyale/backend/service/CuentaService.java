package com.theroyale.backend.service;

import com.theroyale.backend.errors.RecursoNoEncontradoException;
import com.theroyale.backend.model.Cuenta;
import com.theroyale.backend.model.EstadoCuenta;
import com.theroyale.backend.model.EstadoReserva;
import com.theroyale.backend.model.ItemConsumo;
import com.theroyale.backend.model.Operador;
import com.theroyale.backend.model.Pago;
import com.theroyale.backend.model.Servicio;
import com.theroyale.backend.repository.CuentaRepository;
import com.theroyale.backend.repository.ServicioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

// ===== Reglas de la cuenta de consumo de una reserva: servicios cargados, pagos y saldo =====
@Service
@Transactional(readOnly = true)
public class CuentaService {

    private final CuentaRepository cuentaRepository;
    private final ServicioRepository servicioRepository;
    private final OperadorService operadorService;

    public CuentaService(CuentaRepository cuentaRepository,
                         ServicioRepository servicioRepository,
                         OperadorService operadorService) {
        this.cuentaRepository = cuentaRepository;
        this.servicioRepository = servicioRepository;
        this.operadorService = operadorService;
    }

    public Optional<Cuenta> buscarPorReserva(Long reservaId) {
        return cuentaRepository.findByReservaId(reservaId);
    }

    public long contarAbiertas() {
        return cuentaRepository.countByEstado(EstadoCuenta.ABIERTA);
    }

    @Transactional
    public Cuenta agregarConsumo(Long reservaId, Long servicioId, Integer cantidad, Long operadorId) {
        Cuenta cuenta = obtenerCuentaAbierta(reservaId);

        if (cantidad == null || cantidad < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1.");
        }

        Servicio servicio = servicioRepository.findById(servicioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado: " + servicioId));
        Operador operador = operadorService.obtenerOPredeterminado(operadorId);

        // Se guarda el precio vigente como snapshot: si el servicio cambia de precio, el cargo no cambia
        cuenta.getItemsConsumo().add(ItemConsumo.builder()
                .cuenta(cuenta)
                .servicio(servicio)
                .operador(operador)
                .cantidad(cantidad)
                .fechaHora(LocalDateTime.now())
                .precioUnitario(BigDecimal.valueOf(servicio.getPrecio()))
                .pagado(false)
                .build());

        return cuentaRepository.save(cuenta);
    }

    // Permite corregir un cargo equivocado, siempre que los pagos ya hechos sigan cubiertos por la cuenta
    @Transactional
    public Cuenta eliminarConsumo(Long reservaId, Long itemId) {
        Cuenta cuenta = obtenerCuentaAbierta(reservaId);

        ItemConsumo item = cuenta.getItemsConsumo().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Cargo no encontrado: " + itemId));

        if (Boolean.TRUE.equals(item.getPagado())) {
            throw new IllegalStateException("This charge has already been paid and cannot be removed.");
        }
        if (cuenta.getSaldo().subtract(item.getTotal()).signum() < 0) {
            throw new IllegalStateException("Cannot remove charge: registered payments would exceed the account total.");
        }

        cuenta.getItemsConsumo().remove(item);
        return cuentaRepository.save(cuenta);
    }

    @Transactional
    public Cuenta registrarPago(Long reservaId, BigDecimal monto, String metodoPago, Long operadorId) {
        Cuenta cuenta = obtenerCuentaAbierta(reservaId);

        if (monto == null || monto.signum() <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero.");
        }
        if (metodoPago == null || metodoPago.trim().isEmpty()) {
            throw new IllegalArgumentException("Payment method is required.");
        }
        if (monto.compareTo(cuenta.getSaldo()) > 0) {
            throw new IllegalArgumentException("Payment (" + monto + ") exceeds the outstanding balance (" + cuenta.getSaldo() + ").");
        }

        Operador operador = operadorService.obtenerOPredeterminado(operadorId);

        cuenta.getPagos().add(Pago.builder()
                .cuenta(cuenta)
                .operador(operador)
                .monto(monto)
                .fecha(LocalDateTime.now())
                .metodoPago(metodoPago.trim())
                .build());

        if (cuenta.getSaldo().signum() == 0) {
            cuenta.getItemsConsumo().forEach(item -> item.setPagado(true));
        }

        return cuentaRepository.save(cuenta);
    }

    // Services can only be charged to a confirmed or in-progress reservation
    private Cuenta obtenerCuentaAbierta(Long reservaId) {
        Cuenta cuenta = cuentaRepository.findByReservaId(reservaId)
                .orElseThrow(() -> new IllegalStateException("This reservation does not have an account yet: confirm it first."));

        if (cuenta.getEstado() != EstadoCuenta.ABIERTA) {
            throw new IllegalStateException("The account for this reservation is already closed.");
        }

        EstadoReserva estado = cuenta.getReserva().getEstado();
        if (estado != EstadoReserva.CONFIRMADA && estado != EstadoReserva.EN_CURSO) {
            throw new IllegalStateException("Services can only be charged to confirmed or in-progress reservations.");
        }
        return cuenta;
    }
}
