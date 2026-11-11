package com.theroyale.backend.service;

import com.theroyale.backend.dto.CrearResenaRequest;
import com.theroyale.backend.dto.CrearReservaRequest;
import com.theroyale.backend.model.Cliente;
import com.theroyale.backend.model.Cuenta;
import com.theroyale.backend.model.EstadoCuenta;
import com.theroyale.backend.model.EstadoHabitacion;
import com.theroyale.backend.model.EstadoReserva;
import com.theroyale.backend.model.Habitacion;
import com.theroyale.backend.model.Operador;
import com.theroyale.backend.model.Resena;
import com.theroyale.backend.model.Reserva;
import com.theroyale.backend.model.Servicio;
import com.theroyale.backend.model.TipoHabitacion;
import com.theroyale.backend.model.TipoOperador;
import com.theroyale.backend.repository.ClienteRepository;
import com.theroyale.backend.repository.HabitacionRepository;
import com.theroyale.backend.repository.OperadorRepository;
import com.theroyale.backend.repository.ResenaRepository;
import com.theroyale.backend.repository.ReservaRepository;
import com.theroyale.backend.repository.ServicioRepository;
import com.theroyale.backend.repository.TipoHabitacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// ===== Pruebas de integración de ReservaService: sin mocks =====
// Servicios, repositorios y H2 son los reales. El perfil "test" usa su propia base, sin los datos
// del DataLoader, y @Transactional deshace los cambios al terminar cada prueba.
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReservaServiceIntegrationTest {

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private CuentaService cuentaService;

    @Autowired
    private TipoHabitacionRepository tipoHabitacionRepository;

    @Autowired
    private HabitacionRepository habitacionRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ServicioRepository servicioRepository;

    @Autowired
    private OperadorRepository operadorRepository;

    @Autowired
    private ResenaService resenaService;

    @Autowired
    private ResenaRepository resenaRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    private TipoHabitacion suite;
    private Habitacion unicaHabitacion;
    private Cliente cliente;

    // ===== Datos comunes: un tipo con una sola habitación libre, para poder agotar la disponibilidad =====
    @BeforeEach
    void setUp() {
        suite = tipoHabitacionRepository.save(TipoHabitacion.builder().nombre("Suite").build());
        unicaHabitacion = habitacionRepository.save(Habitacion.builder()
                .numero("101")
                .tipoHabitacion(suite)
                .precio(200.0)
                .estado(EstadoHabitacion.DISPONIBLE)
                .build());
        cliente = clienteRepository.save(Cliente.builder()
                .nombre("Ana")
                .apellido("Prueba")
                .email("ana@email.com")
                .password("pass1234")
                .cedula("1001")
                .fechaRegistro(LocalDate.of(2025, 1, 15))
                .build());
    }

    // ===== Reservar con habitación libre =====
    @Test
    void reservaService_crear_conHabitacionLibre_quedaPendienteConHabitacionAsignada() {
        // Arrange
        CrearReservaRequest solicitud = solicitudDesdeHoy(3);

        // Act
        Reserva creada = reservaService.crear(solicitud);

        // Assert
        assertThat(creada.getId()).isNotNull();
        assertThat(creada.getEstado()).isEqualTo(EstadoReserva.PENDIENTE);
        assertThat(creada.getHabitacion().getId()).isEqualTo(unicaHabitacion.getId());
        assertThat(creada.getPrecioNocheAcordado()).isEqualByComparingTo(BigDecimal.valueOf(200));
    }

    // ===== Reservar sin disponibilidad: pasa por la consulta real de solapamiento =====
    @Test
    void reservaService_crear_sinHabitacionesLibres_lanzaExcepcion() {
        // Arrange: la única habitación ya está reservada para las mismas fechas
        reservaService.crear(solicitudDesdeHoy(3));

        // Act & Assert
        assertThatThrownBy(() -> reservaService.crear(solicitudDesdeHoy(2)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Suite");
    }

    // ===== Confirmar abre la cuenta de la reserva =====
    @Test
    void reservaService_confirmar_reservaPendiente_quedaConfirmadaYAbreLaCuenta() {
        // Arrange
        Reserva creada = reservaService.crear(solicitudDesdeHoy(3));

        // Act
        Reserva confirmada = reservaService.confirmar(creada.getId());

        // Assert
        assertThat(confirmada.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
        Cuenta cuenta = cuentaService.buscarPorReserva(creada.getId()).orElseThrow();
        assertThat(cuenta.getEstado()).isEqualTo(EstadoCuenta.ABIERTA);
    }

    // ===== Acabar la estadía solo se permite con la cuenta pagada =====
    @Test
    void reservaService_hacerCheckOut_conSaldoPendiente_lanzaExcepcionYSigueEnCurso() {
        // Arrange: estadía en curso con un servicio cargado y sin pagar
        Reserva reserva = reservaService.crear(solicitudDesdeHoy(3));
        reservaService.confirmar(reserva.getId());
        reservaService.hacerCheckIn(reserva.getId());

        Servicio spa = servicioRepository.save(Servicio.builder()
                .nombre("Spa")
                .descripcion("Masaje relajante")
                .precio(80.0)
                .imagenUrl("/images/spa.webp")
                .build());
        Operador operador = operadorRepository.save(Operador.builder()
                .nombre("Laura")
                .email("laura@theroyale.com")
                .password("operator123")
                .tipo(TipoOperador.OPERADOR)
                .build());
        cuentaService.agregarConsumo(reserva.getId(), spa.getId(), 1, operador.getId());

        // Act & Assert
        assertThatThrownBy(() -> reservaService.hacerCheckOut(reserva.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("outstanding balance");
        assertThat(reservaService.obtenerPorId(reserva.getId()).getEstado()).isEqualTo(EstadoReserva.EN_CURSO);
    }

    // ===== RN-9: borrar una reserva con reseña borra también la reseña, sin error de integridad =====
    @Test
    void reservaService_eliminar_reservaFinalizadaConResena_borraTambienLaResena() {
        // Arrange: estadía completa (confirmar, check-in y check-out sin consumos) y su reseña
        Reserva reserva = reservaService.crear(solicitudDesdeHoy(2));
        reservaService.confirmar(reserva.getId());
        reservaService.hacerCheckIn(reserva.getId());
        reservaService.hacerCheckOut(reserva.getId());
        Resena resena = resenaService.crear(new CrearResenaRequest(reserva.getId(), cliente.getId(), 5,
                "Great stay", "Spotless room and very attentive staff."));
        assertThat(resenaRepository.findById(resena.getId())).isPresent();

        // Act
        reservaService.eliminar(reserva.getId());
        reservaRepository.flush();

        // Assert
        assertThat(resenaRepository.findById(resena.getId())).isEmpty();
        assertThat(resenaRepository.findByReservaId(reserva.getId())).isEmpty();
        assertThat(reservaRepository.findById(reserva.getId())).isEmpty();
        assertThat(cuentaService.buscarPorReserva(reserva.getId())).isEmpty();
    }

    // Empieza hoy porque el servicio rechaza fechas pasadas y el check-in exige que ya sea el día de llegada
    private CrearReservaRequest solicitudDesdeHoy(int noches) {
        LocalDate hoy = LocalDate.now();
        return new CrearReservaRequest(cliente.getId(), suite.getId(), hoy, hoy.plusDays(noches), 2);
    }
}
