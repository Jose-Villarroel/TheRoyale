package com.theroyale.backend.service;

import com.theroyale.backend.errors.RecursoNoEncontradoException;
import com.theroyale.backend.errors.ReglaNegocioException;
import com.theroyale.backend.model.Cuenta;
import com.theroyale.backend.model.EstadoCuenta;
import com.theroyale.backend.model.EstadoHabitacion;
import com.theroyale.backend.model.EstadoReserva;
import com.theroyale.backend.model.Habitacion;
import com.theroyale.backend.model.Reserva;
import com.theroyale.backend.repository.ReservaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.theroyale.backend.dto.CrearReservaRequest;
import com.theroyale.backend.dto.DisponibilidadTipoResponse;
import com.theroyale.backend.dto.TextoPorIdioma;
import com.theroyale.backend.model.Cliente;
import com.theroyale.backend.model.TipoHabitacion;
import com.theroyale.backend.repository.HabitacionRepository;
import com.theroyale.backend.service.plan.PlanVisitaService;
import com.theroyale.backend.service.precio.PrecioService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

// ===== Ciclo de vida de una reserva gestionado por el operador =====
// PENDIENTE -> CONFIRMADA (se abre la cuenta) -> EN_CURSO (check-in) -> FINALIZADA (check-out);
// PENDIENTE / CONFIRMADA -> CANCELADA. Ver EstadoReserva para la equivalencia con el enunciado.
// Para clientes, tipos de habitación y cuentas usa sus servicios (no sus repositorios), de modo que
// las validaciones y los mensajes de "no encontrado" sean los mismos en toda la aplicación.
@Service
@Transactional(readOnly = true)
public class ReservaService {

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private CuentaService cuentaService;

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private TipoHabitacionService tipoHabitacionService;

    @Autowired
    private HabitacionRepository habitacionRepository;

    // Solo para RN-9 (borrar la reseña con la reserva). ResenaService no depende de este servicio
    // (carga las reservas con ReservaRepository), así que no hay ciclo de dependencias.
    @Autowired
    private ResenaService resenaService;

    // El plan de visita se borra con la reserva. PlanVisitaService carga las reservas con
    // ReservaRepository (como ResenaService), así que tampoco hay ciclo de dependencias.
    @Autowired
    private PlanVisitaService planVisitaService;

    // Precio por noche con los precios especiales que aprobó el admin (sin ellos, el precio base)
    @Autowired
    private PrecioService precioService;

      // Habitaciones que no se pueden usar sin importar las fechas
    private static final List<EstadoHabitacion> ESTADOS_HABITACION_NO_UTILIZABLES =
            List.of(EstadoHabitacion.MANTENIMIENTO, EstadoHabitacion.DESHABILITADA);

    // Reservas que ocupan la habitación en su rango de fechas (CANCELADA y FINALIZADA no)
    private static final List<EstadoReserva> ESTADOS_RESERVA_BLOQUEANTES =
            List.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA, EstadoReserva.EN_CURSO);

    // Lo que el cliente ve como "reservas activas" frente a su historial
    private static final List<EstadoReserva> ESTADOS_ACTIVOS =
            List.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA, EstadoReserva.EN_CURSO);

    private static final List<EstadoReserva> ESTADOS_HISTORIAL =
            List.of(EstadoReserva.FINALIZADA, EstadoReserva.CANCELADA);


    public List<Reserva> listar(EstadoReserva estado) {
        return estado == null
                ? reservaRepository.findAllByOrderByFechaInicioDesc()
                : reservaRepository.findByEstadoOrderByFechaInicioDesc(estado);
    }

    // activas = true -> reservas vigentes (AC16); false -> historial (AC17); null -> todas las del cliente
    public List<Reserva> listarPorCliente(Long clienteId, Boolean activas) {
        if (activas == null) {
            return reservaRepository.findByClienteIdOrderByFechaInicioDesc(clienteId);
        }
        return reservaRepository.findByClienteIdAndEstadoInOrderByFechaInicioDesc(
                clienteId, activas ? ESTADOS_ACTIVOS : ESTADOS_HISTORIAL);
    }

    public Reserva obtenerPorId(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.conCodigo("reserva.no-encontrada", id));
    }

    // Busca la reserva activa (CONFIRMADA o EN_CURSO) de una habitacion dado su numero.
    // El operador ingresa el numero de la habitacion para localizar al huesped y agregar consumos.
    public Reserva buscarActivaPorNumeroHabitacion(String numero) {
        return reservaRepository
                .findFirstByHabitacionNumeroAndEstadoIn(numero,
                        List.of(EstadoReserva.CONFIRMADA, EstadoReserva.EN_CURSO))
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No active reservation found for room " + numero));
    }

    // ===== Cuántas habitaciones quedan libres de cada tipo entre dos fechas =====
    // Devuelve todos los tipos, incluidos los que estan agotados, para que el front los marque.
    public List<DisponibilidadTipoResponse> consultarDisponibilidad(LocalDate inicio, LocalDate fin) {
        validarRangoFechas(inicio, fin);

        Locale idioma = LocaleContextHolder.getLocale();
        List<DisponibilidadTipoResponse> disponibilidad = new ArrayList<>();
        for (TipoHabitacion tipo : tipoHabitacionService.listarTodos()) {
            List<Habitacion> libres = habitacionRepository.buscarDisponibles(
                    tipo.getId(), inicio, fin,
                    ESTADOS_HABITACION_NO_UTILIZABLES, ESTADOS_RESERVA_BLOQUEANTES, null);

            // Precio promedio por noche de esas fechas, con los precios que haya aprobado el admin
            BigDecimal precioDesde = libres.stream()
                    .map(habitacion -> precioService.precioPromedio(habitacion, inicio, fin))
                    .min(Comparator.naturalOrder())
                    .orElse(null);

            // El nombre y la descripcion salen en el idioma de la peticion, con respaldo al ingles
            disponibilidad.add(new DisponibilidadTipoResponse(
                    tipo.getId(),
                    TextoPorIdioma.elegir(idioma, tipo.getNombre(), tipo.getNombreEs()),
                    TextoPorIdioma.elegir(idioma, tipo.getDescripcion(), tipo.getDescripcionEs()),
                    tipo.getImagenUrl(),
                    libres.size(), precioDesde, precioService.etiqueta(tipo.getId(), inicio, fin)));
        }
        return disponibilidad;
    }

  // ===== Crea una reserva y le asigna automáticamente una habitación libre del tipo pedido =====
    @Transactional
    public Reserva crear(CrearReservaRequest solicitud) {
        validarSolicitud(solicitud);

        Cliente cliente = clienteService.obtenerPorId(solicitud.clienteId());
        TipoHabitacion tipo = tipoHabitacionService.obtenerPorId(solicitud.tipoHabitacionId());

        Habitacion habitacion = buscarHabitacionDisponible(tipo, solicitud.fechaInicio(), solicitud.fechaFin(), null);

        Reserva reserva = Reserva.builder()
                .cliente(cliente)
                .habitacion(habitacion)
                .fechaInicio(solicitud.fechaInicio())
                .fechaFin(solicitud.fechaFin())
                .estado(EstadoReserva.PENDIENTE)
                .cantidadPersonas(solicitud.cantidadPersonas())
                // Promedio por noche con los precios aprobados: queda congelado en la reserva
                .precioNocheAcordado(precioService.precioPromedio(habitacion, solicitud.fechaInicio(), solicitud.fechaFin()))
                .fechaCreacion(LocalDateTime.now())
                .build();

        return reservaRepository.save(reserva);
    }

    // ===== Cambia fechas, tipo de habitación y número de personas de una reserva ya creada =====
    // La reserva se reescribe en vez de duplicarse: así conserva su cuenta y su historial.
    // Solo se libera la habitación anterior si hay una libre con los datos nuevos; si no, queda intacta.
    @Transactional
    public Reserva modificar(Long id, CrearReservaRequest nuevos) {
        Reserva reserva = obtenerPorId(id);
        exigirEstado(reserva, "reserva.modificar.estado-invalido", EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA);

        if (nuevos == null || nuevos.tipoHabitacionId() == null || nuevos.cantidadPersonas() == null) {
            throw ReglaNegocioException.badRequest("reserva.datos-requeridos-modificar");
        }
        if (nuevos.cantidadPersonas() < 1) {
            throw ReglaNegocioException.badRequest("reserva.huespedes-minimo");
        }
        validarRangoFechas(nuevos.fechaInicio(), nuevos.fechaFin());

        TipoHabitacion tipo = tipoHabitacionService.obtenerPorId(nuevos.tipoHabitacionId());

        // Se excluye esta misma reserva del cálculo: si no, sus propias fechas la bloquearían
        Habitacion habitacion = buscarHabitacionDisponible(tipo, nuevos.fechaInicio(), nuevos.fechaFin(), id);

        reserva.setHabitacion(habitacion);
        reserva.setFechaInicio(nuevos.fechaInicio());
        reserva.setFechaFin(nuevos.fechaFin());
        reserva.setCantidadPersonas(nuevos.cantidadPersonas());
        reserva.setPrecioNocheAcordado(precioService.precioPromedio(habitacion, nuevos.fechaInicio(), nuevos.fechaFin()));

        return reservaRepository.save(reserva);
    }

    @Transactional
    public Reserva confirmar(Long id) {
        Reserva reserva = obtenerPorId(id);
        exigirEstadoOperador(reserva, "confirmar", EstadoReserva.PENDIENTE);

        reserva.setEstado(EstadoReserva.CONFIRMADA);
        Reserva guardada = reservaRepository.save(reserva);

        // La cuenta nace con la reserva confirmada: desde aqui se le pueden cargar servicios
        cuentaService.abrirSiNoExiste(guardada);
        return guardada;
    }

    @Transactional
    public Reserva cancelar(Long id) {
        Reserva reserva = obtenerPorId(id);
        exigirEstado(reserva, "reserva.cancelar.estado-invalido", EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA);

        // Una estadia que ya empezo (o empieza hoy) se cierra con check-out, no se cancela
        if (!reserva.getFechaInicio().isAfter(LocalDate.now())) {
            throw ReglaNegocioException.conflict("reserva.cancelar.fecha-pasada", reserva.getFechaInicio());
        }

        Cuenta cuenta = cuentaService.buscarPorReserva(id).orElse(null);
        if (cuenta != null) {
            if (cuenta.getSaldo().signum() != 0) {
                throw ReglaNegocioException.conflict("reserva.cancelar.saldo-pendiente", cuenta.getSaldo());
            }
            cuenta.setEstado(EstadoCuenta.CERRADA);
        }

        reserva.setEstado(EstadoReserva.CANCELADA);
        return reservaRepository.save(reserva);
    }

    @Transactional
    public Reserva hacerCheckIn(Long id) {
        Reserva reserva = obtenerPorId(id);
        exigirEstadoOperador(reserva, "check in", EstadoReserva.CONFIRMADA);

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

        cuentaService.abrirSiNoExiste(guardada);
        return guardada;
    }

    @Transactional
    public Reserva hacerCheckOut(Long id) {
        Reserva reserva = obtenerPorId(id);
        exigirEstadoOperador(reserva, "check out", EstadoReserva.EN_CURSO);

        Cuenta cuenta = cuentaService.buscarPorReserva(id)
                .orElseThrow(() -> ReglaNegocioException.conflict("reserva.sin-cuenta"));
        if (cuenta.getSaldo().signum() != 0) {
            throw new IllegalStateException("Cannot check out: the account has an outstanding balance of " + cuenta.getSaldo() + ".");
        }

        cuenta.setEstado(EstadoCuenta.CERRADA);
        reserva.getHabitacion().setEstado(EstadoHabitacion.DISPONIBLE);
        reserva.setEstado(EstadoReserva.FINALIZADA);
        return reservaRepository.save(reserva);
    }

    // ===== Borra una reserva junto con su reseña, su plan de visita y su cuenta (consumos y pagos incluidos) =====
    // Una estadía EN_CURSO tiene al huésped en la habitación y una cuenta viva: se cierra con
    // check-out, no se borra. Las demás (pendientes, confirmadas, canceladas o finalizadas) sí.
    @Transactional
    public void eliminar(Long id) {
        Reserva reserva = obtenerPorId(id);
        if (reserva.getEstado() == EstadoReserva.EN_CURSO) {
            throw new IllegalStateException("Cannot delete a reservation that is in progress: check the guest out first.");
        }

        resenaService.eliminarPorReserva(reserva);
        planVisitaService.eliminarPorReserva(reserva);
        cuentaService.eliminarPorReserva(reserva);
        reservaRepository.delete(reserva);
    }

    // Toma la primera habitación libre (ordenadas por número); si no hay ninguna, no se puede reservar
    private Habitacion buscarHabitacionDisponible(TipoHabitacion tipo, LocalDate inicio, LocalDate fin,
                                                  Long reservaIdExcluida) {
        List<Habitacion> disponibles = habitacionRepository.buscarDisponibles(
                tipo.getId(), inicio, fin,
                ESTADOS_HABITACION_NO_UTILIZABLES, ESTADOS_RESERVA_BLOQUEANTES, reservaIdExcluida);

        if (disponibles.isEmpty()) {
            throw ReglaNegocioException.conflict("reserva.sin-disponibilidad", tipo.getNombre(), inicio, fin);
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
            throw ReglaNegocioException.badRequest("reserva.datos-requeridos-crear");
        }
        if (solicitud.cantidadPersonas() < 1) {
            throw ReglaNegocioException.badRequest("reserva.huespedes-minimo");
        }
        validarRangoFechas(solicitud.fechaInicio(), solicitud.fechaFin());
    }

    // Las mismas reglas de fechas valen para crear una reserva y para consultar disponibilidad
    private void validarRangoFechas(LocalDate inicio, LocalDate fin) {
        if (inicio == null || fin == null) {
            throw ReglaNegocioException.badRequest("reserva.fechas-requeridas");
        }
        if (inicio.isBefore(LocalDate.now())) {
            throw ReglaNegocioException.badRequest("reserva.fecha-inicio-pasada");
        }
        if (!fin.isAfter(inicio)) {
            throw ReglaNegocioException.badRequest("reserva.fecha-fin-anterior");
        }
    }

    // Variante del huesped: el codigo del contrato identifica la accion (cancelar o modificar) y el
    // estado viaja como parametro, para que el mensaje se pueda traducir.
    private void exigirEstado(Reserva reserva, String codigo, EstadoReserva... permitidos) {
        if (!estaEn(reserva, permitidos)) {
            throw ReglaNegocioException.conflict(codigo, reserva.getEstado());
        }
    }

    // Variante del operador (confirmar, check-in, check-out): literal en ingles, sin codigo
    private void exigirEstadoOperador(Reserva reserva, String accion, EstadoReserva... permitidos) {
        if (!estaEn(reserva, permitidos)) {
            throw new IllegalStateException("Cannot " + accion + " a reservation with status " + reserva.getEstado() + ".");
        }
    }

    private boolean estaEn(Reserva reserva, EstadoReserva... permitidos) {
        for (EstadoReserva permitido : permitidos) {
            if (reserva.getEstado() == permitido) {
                return true;
            }
        }
        return false;
    }
}
