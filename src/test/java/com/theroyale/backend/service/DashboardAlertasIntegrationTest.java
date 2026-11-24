package com.theroyale.backend.service;

import com.theroyale.backend.dto.dashboard.AlertaResponse;
import com.theroyale.backend.dto.dashboard.OperacionHoyResponse;
import com.theroyale.backend.model.Cliente;
import com.theroyale.backend.model.Cuenta;
import com.theroyale.backend.model.EstadoCuenta;
import com.theroyale.backend.model.EstadoHabitacion;
import com.theroyale.backend.model.EstadoReserva;
import com.theroyale.backend.model.Habitacion;
import com.theroyale.backend.model.ItemConsumo;
import com.theroyale.backend.model.Operador;
import com.theroyale.backend.model.Pago;
import com.theroyale.backend.model.PlanVisita;
import com.theroyale.backend.model.Resena;
import com.theroyale.backend.model.Reserva;
import com.theroyale.backend.model.Servicio;
import com.theroyale.backend.model.TipoHabitacion;
import com.theroyale.backend.model.TipoOperador;
import com.theroyale.backend.repository.ClienteRepository;
import com.theroyale.backend.repository.CuentaRepository;
import com.theroyale.backend.repository.HabitacionRepository;
import com.theroyale.backend.repository.OperadorRepository;
import com.theroyale.backend.repository.PlanVisitaRepository;
import com.theroyale.backend.repository.ResenaRepository;
import com.theroyale.backend.repository.ReservaRepository;
import com.theroyale.backend.repository.ServicioRepository;
import com.theroyale.backend.repository.TipoHabitacionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// =============================================================================================
// ===== Pruebas de las alertas accionables y de la operación del día =====
//
// Van en una clase aparte de DashboardServiceIntegrationTest porque necesitan datos anclados a
// LocalDate.now(): las alertas responden a "sale hoy", "llega en 48 horas" o "reseña de esta
// semana", y con fechas fijas de calendario no se dispararían nunca.
//
// Escenario que arma setUp (hoy = el día en que corre la prueba):
//   A01 y A02 DISPONIBLE, A03 MANTENIMIENTO                      -> inventario = 3
//   R_SALE      EN_CURSO    hoy-2 -> hoy     cuenta ABIERTA: consumo 100, pago 30 -> debe 70
//   R_EN_CASA   EN_CURSO    hoy-1 -> hoy+3
//   R_LLEGA     CONFIRMADA  hoy   -> hoy+2
//   R_PENDIENTE PENDIENTE   hoy+1 -> hoy+3   (llega en menos de 48 h y nadie la confirmó)
//   Reseña de 1 estrella escrita ayer, visible
//   Plan de visita marcado como requiereRevision en una estadía próxima
// =============================================================================================
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DashboardAlertasIntegrationTest {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private TipoHabitacionRepository tipoHabitacionRepository;

    @Autowired
    private HabitacionRepository habitacionRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private CuentaRepository cuentaRepository;

    @Autowired
    private ServicioRepository servicioRepository;

    @Autowired
    private OperadorRepository operadorRepository;

    @Autowired
    private ResenaRepository resenaRepository;

    @Autowired
    private PlanVisitaRepository planVisitaRepository;

    @Autowired
    private EntityManager entityManager;

    private LocalDate hoy;
    private TipoHabitacion tipo;
    private Cliente cliente;
    private Operador operador;
    private Servicio servicio;
    private Habitacion a01;
    private Habitacion a02;

    @BeforeEach
    void setUp() {
        hoy = LocalDate.now();

        tipo = tipoHabitacionRepository.save(TipoHabitacion.builder().nombre("Alert Suite").build());
        a01 = guardarHabitacion("A01", EstadoHabitacion.DISPONIBLE);
        a02 = guardarHabitacion("A02", EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("A03", EstadoHabitacion.MANTENIMIENTO);

        cliente = clienteRepository.save(Cliente.builder()
                .nombre("Luis").apellido("Alerta")
                .email("luis.alertas@email.com").password("pass1234").cedula("ALERT-1")
                .build());

        operador = operadorRepository.save(Operador.builder()
                .nombre("Operador Alertas").email("op.alertas@theroyale.com")
                .password("operator123").tipo(TipoOperador.OPERADOR)
                .build());

        servicio = servicioRepository.save(Servicio.builder()
                .nombre("Alert Spa").descripcion("Servicio de prueba").precio(50.0)
                .imagenUrl("/images/test.webp")
                .build());

        // Sale hoy y debe 70: la alerta de mayor severidad del panel
        Reserva sale = guardarReserva(a01, hoy.minusDays(2), hoy, EstadoReserva.EN_CURSO, 100);
        guardarCuentaConSaldo(sale);

        guardarReserva(a02, hoy.minusDays(1), hoy.plusDays(3), EstadoReserva.EN_CURSO, 100);
        Reserva llega = guardarReserva(a01, hoy, hoy.plusDays(2), EstadoReserva.CONFIRMADA, 100);
        guardarReserva(a02, hoy.plusDays(1), hoy.plusDays(3), EstadoReserva.PENDIENTE, 100);

        // Reseña mala de esta semana
        Reserva pasada = guardarReserva(a01, hoy.minusDays(10), hoy.minusDays(7),
                EstadoReserva.FINALIZADA, 100);
        resenaRepository.save(Resena.builder()
                .reserva(pasada).cliente(cliente).tipoHabitacion(tipo)
                .calificacion(1).titulo("Bad stay").comentario("The room was not ready on arrival.")
                .visible(true).fechaCreacion(hoy.minusDays(1).atTime(10, 0))
                .build());

        // Plan de visita que la validación no pudo verificar, en una estadía que ya llegó
        planVisitaRepository.save(PlanVisita.builder()
                .reserva(llega)
                .preferencias("{}")
                .contenido("{}")
                .requiereRevision(true)
                .fechaGeneracion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .generacionesHoy(1)
                .build());

        // Las consultas nativas no provocan el flush automático de Hibernate
        entityManager.flush();
    }

    // ===== Operación del día =====
    @Test
    void operacionHoy_conLlegadasSalidasYHuespedes_cuentaCadaUnaEnSuCasilla() {
        // Act
        OperacionHoyResponse operacion = dashboardService.operacionHoy();

        // Assert
        assertThat(operacion.fecha()).isEqualTo(hoy);
        assertThat(operacion.llegadas()).isEqualTo(1);   // R_LLEGA empieza hoy
        assertThat(operacion.salidas()).isEqualTo(1);    // R_SALE termina hoy

        // En casa solo R_EN_CASA: quien se va hoy ya no cuenta como alojado (fechaFin > hoy es falso)
        assertThat(operacion.enCasa()).isEqualTo(1);

        assertThat(operacion.salidasConSaldo()).isEqualTo(1);
        assertThat(operacion.saldoPendienteTotal()).isEqualByComparingTo("70");
        assertThat(operacion.habitacionesMantenimiento()).isEqualTo(1);
        assertThat(operacion.habitacionesDeshabilitadas()).isZero();

        // El detalle sirve para cobrar sin salir de la pantalla
        assertThat(operacion.detalleSaldos()).hasSize(1);
        assertThat(operacion.detalleSaldos().get(0).saldo()).isEqualByComparingTo("70");
        assertThat(operacion.detalleSaldos().get(0).habitacionNumero()).isEqualTo("A01");
        assertThat(operacion.detalleSaldos().get(0).cliente()).isEqualTo("Luis Alerta");
    }

    // ===== Las alertas que debe disparar este escenario =====
    @Test
    void alertas_conEscenarioDeRiesgo_disparaLasCuatroQueCorresponden() {
        // Act
        List<AlertaResponse> alertas = dashboardService.alertas();

        // Assert
        assertThat(alertas).extracting(AlertaResponse::codigo)
                .contains("SALIDA_CON_SALDO", "PENDIENTE_POR_CONFIRMAR",
                        "RESENA_NEGATIVA", "PLAN_REQUIERE_REVISION");

        AlertaResponse conSaldo = buscar(alertas, "SALIDA_CON_SALDO");
        assertThat(conSaldo.severidad()).isEqualTo("ALTA");
        assertThat(conSaldo.cantidad()).isEqualTo(1);
        assertThat(conSaldo.detalle()).contains("70");
        assertThat(conSaldo.rutaAccion()).isEqualTo("/admin/reservas");

        assertThat(buscar(alertas, "PENDIENTE_POR_CONFIRMAR").cantidad()).isEqualTo(1);
        assertThat(buscar(alertas, "RESENA_NEGATIVA").cantidad()).isEqualTo(1);
        assertThat(buscar(alertas, "RESENA_NEGATIVA").severidad()).isEqualTo("MEDIA");
        assertThat(buscar(alertas, "PLAN_REQUIERE_REVISION").severidad()).isEqualTo("BAJA");
    }

    // ===== Lo urgente va primero: el administrador lee de arriba hacia abajo =====
    @Test
    void alertas_siempre_vienenOrdenadasDeMayorAMenorSeveridad() {
        // Act
        List<AlertaResponse> alertas = dashboardService.alertas();

        // Assert
        List<Integer> orden = alertas.stream()
                .map(alerta -> switch (alerta.severidad()) {
                    case "ALTA" -> 0;
                    case "MEDIA" -> 1;
                    default -> 2;
                })
                .toList();
        assertThat(orden).isSorted();
    }

    // ===== Mantenimiento: solo molesta avisarlo cuando la semana viene llena =====
    // Con la ocupación baja del escenario base la alerta NO debe aparecer, aunque haya una
    // habitación fuera de servicio: avisar siempre la convertiría en ruido que nadie lee.
    @Test
    void alertas_conHabitacionEnMantenimientoYOcupacionBaja_noAvisa() {
        // Act
        List<AlertaResponse> alertas = dashboardService.alertas();

        // Assert
        assertThat(alertas).extracting(AlertaResponse::codigo).doesNotContain("MANTENIMIENTO_EN_PICO");
    }

    // ===== La misma habitación en mantenimiento sí importa si la semana está casi vendida =====
    // Ojo con la aritmética de esta regla: la ocupación se mide contra un inventario que INCLUYE
    // las habitaciones en mantenimiento (RN-2), así que con k de N fuera de servicio la ocupación
    // no puede pasar de (N-k)/N. Con las 3 habitaciones del escenario base y 1 en mantenimiento el
    // techo es 66 %, y la alerta sería inalcanzable. Por eso aquí se amplía el inventario a 7: con
    // 1 en mantenimiento el techo sube a 85,7 %, que es la proporción real del hotel (50
    // habitaciones, 5 en mantenimiento -> techo del 90 %).
    @Test
    void alertas_conHabitacionEnMantenimientoYSemanaCasiLlena_avisa() {
        // Arrange: 4 habitaciones más (inventario = 7) y las 6 vendibles llenas los próximos 7 días.
        // 7 días x 7 habitaciones = 49 noches posibles; se venden 42 -> 85,71 %, por encima del 85 %.
        List<Habitacion> nuevas = List.of(
                guardarHabitacion("A04", EstadoHabitacion.DISPONIBLE),
                guardarHabitacion("A05", EstadoHabitacion.DISPONIBLE),
                guardarHabitacion("A06", EstadoHabitacion.DISPONIBLE),
                guardarHabitacion("A07", EstadoHabitacion.DISPONIBLE));
        for (Habitacion habitacion : nuevas) {
            guardarReserva(habitacion, hoy, hoy.plusDays(7), EstadoReserva.CONFIRMADA, 100);
        }
        // A01 y A02 se completan desde el día en que quedan libres, sin solaparse con el escenario base
        guardarReserva(a01, hoy.plusDays(2), hoy.plusDays(7), EstadoReserva.CONFIRMADA, 100);
        guardarReserva(a02, hoy.plusDays(3), hoy.plusDays(7), EstadoReserva.CONFIRMADA, 100);
        entityManager.flush();

        // Act
        List<AlertaResponse> alertas = dashboardService.alertas();

        // Assert
        AlertaResponse alerta = buscar(alertas, "MANTENIMIENTO_EN_PICO");
        assertThat(alerta).isNotNull();
        assertThat(alerta.severidad()).isEqualTo("ALTA");
        assertThat(alerta.cantidad()).isEqualTo(1);       // la A03
        assertThat(alerta.rutaAccion()).isEqualTo("/admin/habitaciones");
        assertThat(alerta.detalle()).contains("85");
    }

    // ===== Una caída solo se avisa si hubo con qué comparar =====
    // El escenario no tiene historial anterior, así que la ocupación subió desde cero: avisar de
    // una caída aquí sería un falso positivo.
    @Test
    void alertas_sinHistorialPrevio_noAvisaDeCaidas() {
        // Act
        List<AlertaResponse> alertas = dashboardService.alertas();

        // Assert
        assertThat(alertas).extracting(AlertaResponse::codigo)
                .doesNotContain("CAIDA_OCUPACION", "CAIDA_INGRESOS");
    }

    // ===== Utilidades =====

    private AlertaResponse buscar(List<AlertaResponse> alertas, String codigo) {
        return alertas.stream()
                .filter(alerta -> alerta.codigo().equals(codigo))
                .findFirst()
                .orElse(null);
    }

    private Habitacion guardarHabitacion(String numero, EstadoHabitacion estado) {
        return habitacionRepository.save(Habitacion.builder()
                .numero(numero).tipoHabitacion(tipo).precio(100.0).estado(estado)
                .build());
    }

    private Reserva guardarReserva(Habitacion habitacion, LocalDate inicio, LocalDate fin,
                                   EstadoReserva estado, int precioNoche) {
        return reservaRepository.save(Reserva.builder()
                .cliente(cliente).habitacion(habitacion)
                .fechaInicio(inicio).fechaFin(fin).estado(estado)
                .cantidadPersonas(2)
                .precioNocheAcordado(BigDecimal.valueOf(precioNoche))
                .fechaCreacion(inicio.minusDays(5).atTime(9, 0))
                .build());
    }

    // Cuenta abierta con 100 consumidos y 30 pagados: deja 70 de saldo
    private void guardarCuentaConSaldo(Reserva reserva) {
        Cuenta cuenta = Cuenta.builder()
                .reserva(reserva).estado(EstadoCuenta.ABIERTA)
                .fechaCreacion(reserva.getFechaInicio().atTime(15, 0))
                .itemsConsumo(new ArrayList<>()).pagos(new ArrayList<>())
                .build();

        cuenta.getItemsConsumo().add(ItemConsumo.builder()
                .cuenta(cuenta).servicio(servicio).operador(operador)
                .cantidad(2).precioUnitario(BigDecimal.valueOf(50))
                .fechaHora(LocalDateTime.now().minusHours(5)).pagado(false)
                .build());

        cuenta.getPagos().add(Pago.builder()
                .cuenta(cuenta).operador(operador)
                .monto(BigDecimal.valueOf(30)).fecha(LocalDateTime.now().minusHours(2))
                .metodoPago("CARD")
                .build());

        cuentaRepository.save(cuenta);
    }
}
