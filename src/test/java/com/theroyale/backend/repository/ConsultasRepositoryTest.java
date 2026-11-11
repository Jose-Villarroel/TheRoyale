package com.theroyale.backend.repository;

import com.theroyale.backend.model.Cliente;
import com.theroyale.backend.model.EstadoHabitacion;
import com.theroyale.backend.model.EstadoReserva;
import com.theroyale.backend.model.Habitacion;
import com.theroyale.backend.model.Reserva;
import com.theroyale.backend.model.TipoHabitacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// ===== Pruebas de las consultas escritas a mano con @Query =====
// Una por consulta: 1 de HabitacionRepository y 4 de ReservaRepository.
// Las fechas son fijas porque aquí se prueba el JPQL, no las reglas de negocio del servicio.
@DataJpaTest
class ConsultasRepositoryTest {

    @Autowired
    private HabitacionRepository habitacionRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private TipoHabitacionRepository tipoHabitacionRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    private TipoHabitacion suite;
    private Habitacion habitacion102;
    private Cliente ana;

    // Reservas que las pruebas esperan encontrar
    private Reserva pendienteJunioAna;
    private Reserva confirmadaJulioAna;
    private Reserva enCursoAgostoLuis;
    private Reserva pendienteSeptiembreLuis;

    // ===== Datos comunes: 3 habitaciones, 2 clientes y reservas en distintos estados y fechas =====
    @BeforeEach
    void setUp() {
        suite = tipoHabitacionRepository.save(TipoHabitacion.builder().nombre("Suite").build());

        Habitacion habitacion101 = guardarHabitacion("101", EstadoHabitacion.DISPONIBLE);
        habitacion102 = guardarHabitacion("102", EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("103", EstadoHabitacion.MANTENIMIENTO);

        ana = guardarCliente("Ana", "ana@email.com", "1001");
        Cliente luis = guardarCliente("Luis", "luis@email.com", "1002");

        pendienteJunioAna = guardarReserva(ana, habitacion101, "2030-06-10", "2030-06-15", EstadoReserva.PENDIENTE);
        confirmadaJulioAna = guardarReserva(ana, habitacion101, "2030-07-01", "2030-07-05", EstadoReserva.CONFIRMADA);
        guardarReserva(ana, habitacion101, "2030-01-01", "2030-01-03", EstadoReserva.FINALIZADA);
        guardarReserva(ana, habitacion101, "2030-08-03", "2030-08-05", EstadoReserva.FINALIZADA);

        enCursoAgostoLuis = guardarReserva(luis, habitacion102, "2030-08-01", "2030-08-05", EstadoReserva.EN_CURSO);
        // Cancelada a propósito en las mismas fechas de junio: no debe bloquear ni contar como llegada
        guardarReserva(luis, habitacion102, "2030-06-10", "2030-06-12", EstadoReserva.CANCELADA);
        pendienteSeptiembreLuis = guardarReserva(luis, habitacion102, "2030-09-01", "2030-09-05", EstadoReserva.PENDIENTE);
    }

    // ===== 1. Disponibilidad: la consulta central para poder reservar =====
    @Test
    void habitacionRepository_buscarDisponibles_excluyeOcupadasYEnMantenimiento() {
        // Arrange: la 101 tiene una reserva pendiente en esas fechas, la 102 solo una cancelada
        // y la 103 está en mantenimiento
        LocalDate inicio = LocalDate.parse("2030-06-11");
        LocalDate fin = LocalDate.parse("2030-06-13");

        // Act
        List<Habitacion> disponibles = habitacionRepository.buscarDisponibles(
                suite.getId(), inicio, fin,
                List.of(EstadoHabitacion.MANTENIMIENTO, EstadoHabitacion.DESHABILITADA),
                List.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA, EstadoReserva.EN_CURSO),
                null);

        // Assert
        assertThat(disponibles).containsExactly(habitacion102);
    }

    // ===== 2. Filtro por estado =====
    @Test
    void reservaRepository_findByEstado_retornaSoloEseEstadoEnOrdenDescendente() {
        // Arrange: el setUp guardó 2 reservas pendientes (junio y septiembre)

        // Act
        List<Reserva> pendientes = reservaRepository.findByEstadoOrderByFechaInicioDesc(EstadoReserva.PENDIENTE);

        // Assert: primero la de fecha más lejana
        assertThat(pendientes).containsExactly(pendienteSeptiembreLuis, pendienteJunioAna);
    }

    // ===== 3. Reservas activas de un cliente =====
    @Test
    void reservaRepository_findByClienteIdAndEstadoIn_retornaSoloLasDelClienteConEsosEstados() {
        // Arrange: Ana también tiene reservas finalizadas, y Luis tiene pendientes que no son de ella
        List<EstadoReserva> estadosActivos = List.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA);

        // Act
        List<Reserva> activas = reservaRepository
                .findByClienteIdAndEstadoInOrderByFechaInicioDesc(ana.getId(), estadosActivos);

        // Assert
        assertThat(activas).containsExactly(confirmadaJulioAna, pendienteJunioAna);
    }

    // ===== 4. Llegadas de un día =====
    @Test
    void reservaRepository_findByFechaInicioAndEstadoIn_noIncluyeLasCanceladas() {
        // Arrange: el 10 de junio empiezan dos reservas, pero una está cancelada
        LocalDate fecha = LocalDate.parse("2030-06-10");

        // Act
        List<Reserva> llegadas = reservaRepository.findByFechaInicioAndEstadoIn(
                fecha, List.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA));

        // Assert
        assertThat(llegadas).containsExactly(pendienteJunioAna);
    }

    // ===== 5. Salidas de un día =====
    @Test
    void reservaRepository_findByFechaFinAndEstado_retornaSoloLasEnCurso() {
        // Arrange: el 5 de agosto terminan dos reservas, pero una ya está finalizada
        LocalDate fecha = LocalDate.parse("2030-08-05");

        // Act
        List<Reserva> salidas = reservaRepository.findByFechaFinAndEstado(fecha, EstadoReserva.EN_CURSO);

        // Assert
        assertThat(salidas).containsExactly(enCursoAgostoLuis);
    }

    // ===== Métodos de ayuda: evitan repetir los builders en el setUp =====
    private Habitacion guardarHabitacion(String numero, EstadoHabitacion estado) {
        return habitacionRepository.save(Habitacion.builder()
                .numero(numero)
                .tipoHabitacion(suite)
                .precio(200.0)
                .estado(estado)
                .build());
    }

    private Cliente guardarCliente(String nombre, String email, String cedula) {
        return clienteRepository.save(Cliente.builder()
                .nombre(nombre)
                .apellido("Prueba")
                .email(email)
                .password("pass1234")
                .cedula(cedula)
                .fechaRegistro(LocalDate.of(2025, 1, 15))
                .build());
    }

    private Reserva guardarReserva(Cliente cliente, Habitacion habitacion, String inicio, String fin,
                                   EstadoReserva estado) {
        return reservaRepository.save(Reserva.builder()
                .cliente(cliente)
                .habitacion(habitacion)
                .fechaInicio(LocalDate.parse(inicio))
                .fechaFin(LocalDate.parse(fin))
                .estado(estado)
                .cantidadPersonas(2)
                .precioNocheAcordado(BigDecimal.valueOf(200))
                .fechaCreacion(LocalDateTime.of(2025, 1, 15, 10, 0))
                .build());
    }
}
