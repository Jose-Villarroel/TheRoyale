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
            throw new IllegalArgumentException("La cantidad debe ser al menos 1.");
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
            throw new IllegalStateException("Ese cargo ya esta pagado y no se puede eliminar.");
        }
        if (cuenta.getSaldo().subtract(item.getTotal()).signum() < 0) {
            throw new IllegalStateException("No se puede eliminar el cargo: los pagos registrados superarian el total de la cuenta.");
        }

        cuenta.getItemsConsumo().remove(item);
        return cuentaRepository.save(cuenta);
    }

    @Transactional
    public Cuenta registrarPago(Long reservaId, BigDecimal monto, String metodoPago, Long operadorId) {
        Cuenta cuenta = obtenerCuentaAbierta(reservaId);

        if (monto == null || monto.signum() <= 0) {
            throw new IllegalArgumentException("El monto del pago debe ser mayor que cero.");
        }
        if (metodoPago == null || metodoPago.trim().isEmpty()) {
            throw new IllegalArgumentException("El metodo de pago es obligatorio.");
        }
        if (monto.compareTo(cuenta.getSaldo()) > 0) {
            throw new IllegalArgumentException("El pago (" + monto + ") supera el saldo pendiente (" + cuenta.getSaldo() + ").");
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

    // Los servicios solo se cargan a una reserva confirmada o con el huesped en la habitacion
    private Cuenta obtenerCuentaAbierta(Long reservaId) {
        Cuenta cuenta = cuentaRepository.findByReservaId(reservaId)
                .orElseThrow(() -> new IllegalStateException("La reserva aun no tiene cuenta: primero hay que confirmarla."));

        if (cuenta.getEstado() != EstadoCuenta.ABIERTA) {
            throw new IllegalStateException("La cuenta de esta reserva ya esta cerrada.");
        }

        EstadoReserva estado = cuenta.getReserva().getEstado();
        if (estado != EstadoReserva.CONFIRMADA && estado != EstadoReserva.EN_CURSO) {
            throw new IllegalStateException("Solo se pueden cargar servicios a reservas confirmadas o en curso.");
        }
        return cuenta;
    }
}
