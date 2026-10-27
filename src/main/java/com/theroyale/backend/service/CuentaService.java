package com.theroyale.backend.service;

import com.theroyale.backend.dto.CuentaResponse;
import com.theroyale.backend.errors.RecursoNoEncontradoException;
import com.theroyale.backend.model.Cuenta;
import com.theroyale.backend.model.EstadoCuenta;
import com.theroyale.backend.model.EstadoReserva;
import com.theroyale.backend.model.ItemConsumo;
import com.theroyale.backend.model.Operador;
import com.theroyale.backend.model.Pago;
import com.theroyale.backend.model.Reserva;
import com.theroyale.backend.model.Servicio;
import com.theroyale.backend.repository.CuentaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// ===== Reglas de la cuenta de consumo de una reserva: servicios cargados, pagos y saldo =====
@Service
@Transactional(readOnly = true)
public class CuentaService {

    // Únicos medios de pago que acepta el hotel; la vista del operador arma su selector con esta lista
    public static final List<String> METODOS_PAGO = List.of("CASH", "CARD", "TRANSFER");

    @Autowired
    private CuentaRepository cuentaRepository;

    @Autowired
    private InterfaceService servicioService;

    @Autowired
    private OperadorService operadorService;

    public Optional<Cuenta> buscarPorReserva(Long reservaId) {
        return cuentaRepository.findByReservaId(reservaId);
    }

    // El mapeo a DTO ocurre dentro de la transaccion: items, pagos y sus operadores son LAZY
    public CuentaResponse obtenerRespuestaPorReserva(Long reservaId) {
        return CuentaResponse.desde(cuentaRepository.findByReservaId(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "La reserva " + reservaId + " no tiene cuenta asociada.")));
    }

    public List<String> listarMetodosPago() {
        return METODOS_PAGO;
    }

    public long contarAbiertas() {
        return cuentaRepository.countByEstado(EstadoCuenta.ABIERTA);
    }

    @Transactional
    public CuentaResponse agregarConsumo(Long reservaId, Long servicioId, Integer cantidad, Long operadorId) {
        Cuenta cuenta = obtenerCuentaAbierta(reservaId);

        if (cantidad == null || cantidad < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1.");
        }

        if (servicioId == null) {
            throw new IllegalArgumentException("Service is required.");
        }
        Servicio servicio = servicioService.buscarPorId(servicioId);
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

        return CuentaResponse.desde(cuentaRepository.save(cuenta));
    }

    // Permite corregir un cargo equivocado, siempre que los pagos ya hechos sigan cubiertos por la cuenta
    @Transactional
    public CuentaResponse eliminarConsumo(Long reservaId, Long itemId) {
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
        return CuentaResponse.desde(cuentaRepository.save(cuenta));
    }

    @Transactional
    public CuentaResponse registrarPago(Long reservaId, BigDecimal monto, String metodoPago, Long operadorId) {
        Cuenta cuenta = obtenerCuentaAbierta(reservaId);

        if (monto == null || monto.signum() <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero.");
        }
        if (metodoPago == null || metodoPago.trim().isEmpty()) {
            throw new IllegalArgumentException("Payment method is required.");
        }
        String metodo = metodoPago.trim().toUpperCase();
        if (!METODOS_PAGO.contains(metodo)) {
            throw new IllegalArgumentException("Invalid payment method: " + metodoPago
                    + ". Allowed methods: " + String.join(", ", METODOS_PAGO) + ".");
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
                .metodoPago(metodo)
                .build());

        if (cuenta.getSaldo().signum() == 0) {
            cuenta.getItemsConsumo().forEach(item -> item.setPagado(true));
        }

        return CuentaResponse.desde(cuentaRepository.save(cuenta));
    }

    // ===== Operaciones que dispara el ciclo de vida de la reserva (ReservaService) =====
    // La cuenta nace al confirmar o al hacer check-in, lo que ocurra primero.
    @Transactional
    public Cuenta abrirSiNoExiste(Reserva reserva) {
        return cuentaRepository.findByReservaId(reserva.getId())
                .orElseGet(() -> cuentaRepository.save(Cuenta.builder()
                        .reserva(reserva)
                        .estado(EstadoCuenta.ABIERTA)
                        .fechaCreacion(LocalDateTime.now())
                        .build()));
    }

    // Borra la cuenta con sus consumos y pagos (cascade ALL + orphanRemoval en Cuenta).
    // Se hace flush para que el DELETE salga antes que el de la reserva, dueña de la FK reserva_id.
    @Transactional
    public void eliminarPorReserva(Reserva reserva) {
        cuentaRepository.findByReservaId(reserva.getId()).ifPresent(cuenta -> {
            reserva.setCuenta(null);
            cuentaRepository.delete(cuenta);
            cuentaRepository.flush();
        });
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
