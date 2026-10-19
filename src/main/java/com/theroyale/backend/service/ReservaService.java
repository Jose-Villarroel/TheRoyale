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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.theroyale.backend.dto.CrearReservaRequest;
import com.theroyale.backend.dto.DisponibilidadTipoResponse;
import com.theroyale.backend.model.Cliente;
import com.theroyale.backend.model.TipoHabitacion;
import com.theroyale.backend.repository.ClienteRepository;
import com.theroyale.backend.repository.HabitacionRepository;
import com.theroyale.backend.repository.TipoHabitacionRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;

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

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private CuentaRepository cuentaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private TipoHabitacionRepository tipoHabitacionRepository;

    @Autowired
    private HabitacionRepository habitacionRepository;

      // Habitaciones que no se pueden usar sin importar las fechas
    private static final List<EstadoHabitacion> ESTADOS_HABITACION_NO_UTILIZABLES =
            List.of(EstadoHabitacion.MANTENIMIENTO, EstadoHabitacion.DESHABILITADA);

    // Reservas que ocupan la habitación en su rango de fechas (CANCELADA y FINALIZADA no)
    private static final List<EstadoReserva> ESTADOS_RESERVA_BLOQUEANTES =
            List.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA, EstadoReserva.EN_CURSO);


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

    // ===== Cuántas habitaciones quedan libres de cada tipo entre dos fechas =====
    // Devuelve todos los tipos, incluidos los que estan agotados, para que el front los marque.
    public List<DisponibilidadTipoResponse> consultarDisponibilidad(LocalDate inicio, LocalDate fin) {
        validarRangoFechas(inicio, fin);

        List<DisponibilidadTipoResponse> disponibilidad = new ArrayList<>();
        for (TipoHabitacion tipo : tipoHabitacionRepository.findAll()) {
            List<Habitacion> libres = habitacionRepository.buscarDisponibles(
                    tipo.getId(), inicio, fin,
                    ESTADOS_HABITACION_NO_UTILIZABLES, ESTADOS_RESERVA_BLOQUEANTES);

            BigDecimal precioDesde = libres.stream()
                    .map(habitacion -> BigDecimal.valueOf(habitacion.getPrecio()))
                    .min(Comparator.naturalOrder())
                    .orElse(null);

            disponibilidad.add(new DisponibilidadTipoResponse(
                    tipo.getId(), tipo.getNombre(), tipo.getDescripcion(), tipo.getImagenUrl(),
                    libres.size(), precioDesde));
        }
        return disponibilidad;
    }

  // ===== Crea una reserva y le asigna automáticamente una habitación libre del tipo pedido =====
    @Transactional
    public Reserva crear(CrearReservaRequest solicitud) {
        validarSolicitud(solicitud);

        Cliente cliente = clienteRepository.findById(solicitud.clienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Cliente no encontrado: " + solicitud.clienteId()));

        TipoHabitacion tipo = tipoHabitacionRepository.findById(solicitud.tipoHabitacionId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Tipo de habitación no encontrado: " + solicitud.tipoHabitacionId()));

        Habitacion habitacion = buscarHabitacionDisponible(tipo, solicitud.fechaInicio(), solicitud.fechaFin());

        Reserva reserva = Reserva.builder()
                .cliente(cliente)
                .habitacion(habitacion)
                .fechaInicio(solicitud.fechaInicio())
                .fechaFin(solicitud.fechaFin())
                .estado(EstadoReserva.PENDIENTE)
                .cantidadPersonas(solicitud.cantidadPersonas())
                .precioNocheAcordado(BigDecimal.valueOf(habitacion.getPrecio()))
                .fechaCreacion(LocalDateTime.now())
                .build();

        return reservaRepository.save(reserva);
    }

    @Transactional
    public Reserva confirmar(Long id) {
        Reserva reserva = obtenerPorId(id);
        exigirEstado(reserva, "confirmar", EstadoReserva.PENDIENTE);

        reserva.setEstado(EstadoReserva.CONFIRMADA);
        Reserva guardada = reservaRepository.save(reserva);

        // La cuenta nace con la reserva confirmada: desde aqui se le pueden cargar servicios
        abrirCuentaSiNoExiste(guardada);
        return guardada;
    }

    @Transactional
    public Reserva cancelar(Long id) {
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
        return reservaRepository.save(reserva);
    }

    @Transactional
    public Reserva hacerCheckIn(Long id) {
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
        Reserva guardada = reservaRepository.save(reserva);

        abrirCuentaSiNoExiste(guardada);
        return guardada;
    }

    @Transactional
    public Reserva hacerCheckOut(Long id) {
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
        return reservaRepository.save(reserva);
    }

 // Toma la primera habitación libre (ordenadas por número); si no hay ninguna, no se puede reservar
    private Habitacion buscarHabitacionDisponible(TipoHabitacion tipo, LocalDate inicio, LocalDate fin) {
        List<Habitacion> disponibles = habitacionRepository.buscarDisponibles(
                tipo.getId(), inicio, fin,
                ESTADOS_HABITACION_NO_UTILIZABLES, ESTADOS_RESERVA_BLOQUEANTES);

        if (disponibles.isEmpty()) {
            throw new IllegalStateException("No " + tipo.getNombre() + " rooms are available from "
                    + inicio + " to " + fin + ".");
        }
        return disponibles.get(0);
    }

    private void validarSolicitud(CrearReservaRequest solicitud) {
        if (solicitud == null
                || solicitud.clienteId() == null
                || solicitud.tipoHabitacionId() == null
                || solicitud.fechaInicio() == null
                || solicitud.fechaFin() == null
                || solicitud.cantidadPersonas() == null) {
            throw new IllegalArgumentException("Some required data is missing to create the reservation.");
        }
        if (solicitud.cantidadPersonas() < 1) {
            throw new IllegalArgumentException("The number of guests must be at least 1.");
        }
        validarRangoFechas(solicitud.fechaInicio(), solicitud.fechaFin());
    }

    // Las mismas reglas de fechas valen para crear una reserva y para consultar disponibilidad
    private void validarRangoFechas(LocalDate inicio, LocalDate fin) {
        if (inicio == null || fin == null) {
            throw new IllegalArgumentException("Both the check-in and check-out dates are required.");
        }
        if (inicio.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("The check-in date cannot be in the past.");
        }
        if (!fin.isAfter(inicio)) {
            throw new IllegalArgumentException("The check-out date must be after the check-in date.");
        }
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
