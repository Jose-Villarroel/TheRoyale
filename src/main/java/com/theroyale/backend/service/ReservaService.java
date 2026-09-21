package com.theroyale.backend.service;

import com.theroyale.backend.errors.RecursoNoEncontradoException;
import com.theroyale.backend.model.Cuenta;
import com.theroyale.backend.model.EstadoCuenta;
import com.theroyale.backend.model.EstadoHabitacion;
import com.theroyale.backend.model.EstadoReserva;
import com.theroyale.backend.model.Habitacion;
import com.theroyale.backend.model.Reserva;
import com.theroyale.backend.repository.CuentaRepository;
import com.theroyale.backend.repository.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// ===== Ciclo de vida de una reserva gestionado por el operador =====
// PENDIENTE -> CONFIRMADA (se abre la cuenta) -> EN_CURSO (check-in) -> FINALIZADA (check-out);
// PENDIENTE / CONFIRMADA -> CANCELADA.
@Service
@Transactional(readOnly = true)
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final CuentaRepository cuentaRepository;
    private final OperadorService operadorService;

    public ReservaService(ReservaRepository reservaRepository,
                          CuentaRepository cuentaRepository,
                          OperadorService operadorService) {
        this.reservaRepository = reservaRepository;
        this.cuentaRepository = cuentaRepository;
        this.operadorService = operadorService;
    }

    public List<Reserva> listar(EstadoReserva estado) {
        return estado == null
                ? reservaRepository.findAllByOrderByFechaInicioDesc()
                : reservaRepository.findByEstadoOrderByFechaInicioDesc(estado);
    }

    public Reserva obtenerPorId(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada: " + id));
    }

    public long contarPorEstado(EstadoReserva estado) {
        return reservaRepository.countByEstado(estado);
    }

    // Cantidad de reservas por estado (para las pestanas de filtro del panel)
    public Map<EstadoReserva, Long> contarTodasPorEstado() {
        Map<EstadoReserva, Long> conteos = new EnumMap<>(EstadoReserva.class);
        for (EstadoReserva estado : EstadoReserva.values()) {
            conteos.put(estado, reservaRepository.countByEstado(estado));
        }
        return conteos;
    }

    public List<Reserva> llegadasDeHoy() {
        return reservaRepository.findByFechaInicioAndEstadoIn(
                LocalDate.now(), List.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA));
    }

    public List<Reserva> salidasDeHoy() {
        return reservaRepository.findByFechaFinAndEstado(LocalDate.now(), EstadoReserva.EN_CURSO);
    }

    @Transactional
    public Reserva confirmar(Long id, Long operadorId) {
        Reserva reserva = obtenerPorId(id);
        exigirEstado(reserva, "confirmar", EstadoReserva.PENDIENTE);

        reserva.setEstado(EstadoReserva.CONFIRMADA);
        reserva.setOperador(operadorService.obtenerOPredeterminado(operadorId));
        Reserva guardada = reservaRepository.save(reserva);

        // La cuenta nace con la reserva confirmada: desde aqui se le pueden cargar servicios
        abrirCuentaSiNoExiste(guardada);
        return guardada;
    }

    @Transactional
    public Reserva cancelar(Long id, Long operadorId) {
        Reserva reserva = obtenerPorId(id);
        exigirEstado(reserva, "cancel", EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA);

        Cuenta cuenta = cuentaRepository.findByReservaId(id).orElse(null);
        if (cuenta != null) {
            if (cuenta.getSaldo().signum() != 0) {
                throw new IllegalStateException("Cannot cancel: the account has an outstanding balance of "
                        + cuenta.getSaldo() + ". Remove the charges or register a payment first.");
            }
            cuenta.setEstado(EstadoCuenta.CERRADA);
        }

        reserva.setEstado(EstadoReserva.CANCELADA);
        reserva.setOperador(operadorService.obtenerOPredeterminado(operadorId));
        return reservaRepository.save(reserva);
    }

    @Transactional
    public Reserva hacerCheckIn(Long id, Long operadorId) {
        Reserva reserva = obtenerPorId(id);
        exigirEstado(reserva, "check in", EstadoReserva.CONFIRMADA);

        if (reserva.getFechaInicio().isAfter(LocalDate.now())) {
            throw new IllegalStateException("Check-in cannot be done before the arrival date (" + reserva.getFechaInicio() + ").");
        }

        Habitacion habitacion = reserva.getHabitacion();
        if (habitacion.getEstado() != EstadoHabitacion.DISPONIBLE) {
            throw new IllegalStateException("Room " + habitacion.getNumero() + " is not available (status: " + habitacion.getEstado() + ").");
        }

        habitacion.setEstado(EstadoHabitacion.OCUPADA);
        reserva.setEstado(EstadoReserva.EN_CURSO);
        reserva.setOperador(operadorService.obtenerOPredeterminado(operadorId));
        Reserva guardada = reservaRepository.save(reserva);

        abrirCuentaSiNoExiste(guardada);
        return guardada;
    }

    @Transactional
    public Reserva hacerCheckOut(Long id, Long operadorId) {
        Reserva reserva = obtenerPorId(id);
        exigirEstado(reserva, "check out", EstadoReserva.EN_CURSO);

        Cuenta cuenta = cuentaRepository.findByReservaId(id)
                .orElseThrow(() -> new IllegalStateException("This reservation has no associated account."));
        if (cuenta.getSaldo().signum() != 0) {
            throw new IllegalStateException("Cannot check out: the account has an outstanding balance of " + cuenta.getSaldo() + ".");
        }

        cuenta.setEstado(EstadoCuenta.CERRADA);
        reserva.getHabitacion().setEstado(EstadoHabitacion.DISPONIBLE);
        reserva.setEstado(EstadoReserva.FINALIZADA);
        reserva.setOperador(operadorService.obtenerOPredeterminado(operadorId));
        return reservaRepository.save(reserva);
    }

    private void abrirCuentaSiNoExiste(Reserva reserva) {
        if (cuentaRepository.findByReservaId(reserva.getId()).isEmpty()) {
            cuentaRepository.save(Cuenta.builder()
                    .reserva(reserva)
                    .estado(EstadoCuenta.ABIERTA)
                    .fechaCreacion(LocalDateTime.now())
                    .build());
        }
    }

    private void exigirEstado(Reserva reserva, String accion, EstadoReserva... permitidos) {
        for (EstadoReserva permitido : permitidos) {
            if (reserva.getEstado() == permitido) {
                return;
            }
        }
        throw new IllegalStateException("Cannot " + accion + " a reservation with status " + reserva.getEstado() + ".");
    }
}
