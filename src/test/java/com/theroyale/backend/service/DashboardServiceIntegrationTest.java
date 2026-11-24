package com.theroyale.backend.service;

import com.theroyale.backend.dto.dashboard.DesgloseResponse;
import com.theroyale.backend.dto.dashboard.KpisResponse;
import com.theroyale.backend.dto.dashboard.PuntoIngresosResponse;
import com.theroyale.backend.dto.dashboard.PuntoOcupacionResponse;
import com.theroyale.backend.dto.dashboard.ResumenResponse;
import com.theroyale.backend.model.Cliente;
import com.theroyale.backend.model.Cuenta;
import com.theroyale.backend.model.EstadoCuenta;
import com.theroyale.backend.model.EstadoHabitacion;
import com.theroyale.backend.model.EstadoReserva;
import com.theroyale.backend.model.Habitacion;
import com.theroyale.backend.model.ItemConsumo;
import com.theroyale.backend.model.Operador;
import com.theroyale.backend.model.Pago;
import com.theroyale.backend.model.Reserva;
import com.theroyale.backend.model.Servicio;
import com.theroyale.backend.model.TipoHabitacion;
import com.theroyale.backend.model.TipoOperador;
import com.theroyale.backend.repository.ClienteRepository;
import com.theroyale.backend.repository.CuentaRepository;
import com.theroyale.backend.repository.HabitacionRepository;
import com.theroyale.backend.repository.OperadorRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// =============================================================================================
// ===== Pruebas de integración de DashboardService: sin mocks =====
//
// Se prueba contra H2 real porque lo que hay que verificar ES el SQL: el recorte de las noches al
// período, los GROUP BY y las divisiones de los indicadores. Con repositorios mockeados estas
// pruebas pasarían siempre y no dirían nada sobre si la ocupación está bien calculada.
//
// El perfil "test" usa su propia base y no ejecuta el DataLoader (es @Profile("default")), así que
// el único dato existente es el que arma setUp. @Transactional lo deshace al terminar cada prueba.
//
// ===== El dataset y sus resultados, calculados a mano =====
//
// Período de las pruebas: del 1 al 10 de marzo de 2026 (10 días).
//
// Habitaciones: T01 y T02 DISPONIBLE, T03 MANTENIMIENTO, T04 DESHABILITADA
//   -> inventario = 3 (RN-2: solo DESHABILITADA sale del inventario; MANTENIMIENTO sigue dentro)
//   -> noches disponibles = 3 habitaciones x 10 días = 30
//
// Reservas (precio por noche entre paréntesis):
//   R1  T01  01mar -> 04mar  FINALIZADA (100)  creada 20feb   3 noches dentro =  300
//   R2  T02  27feb -> 03mar  CONFIRMADA (200)  creada 20feb   2 noches dentro =  400  (recortada)
//   R3  T01  05mar -> 07mar  CANCELADA  (500)  creada 05mar   no se vende
//   R4  T02  08mar -> 12mar  EN_CURSO    (50)  creada 26feb   3 noches dentro =  150  (recortada)
//   R5  T01  15mar -> 18mar  CONFIRMADA (400)  creada 25feb   fuera del período
//   R6  T02  02mar -> 04mar  PENDIENTE  (999)  creada 25feb   no se vende (sin confirmar)
//
//   noches vendidas   = 3 + 2 + 3 = 8
//   ingresos habitación = 300 + 400 + 150 = 850
//   ocupación   = 8 / 30            = 26,67 %
//   ADR         = 850 / 8           = 106,25
//   RevPAR      = 850 / 30          =  28,33
//   ALOS        = (3 + 4 + 4) / 3   =   3,67 noches  (noches completas, no recortadas)
//   lead time   = (9 + 7 + 10) / 3  =   8,67 días
//
// Cuentas:
//   A (de R1, ABIERTA)  consumo 2 x 25 = 50 el 02mar   pago 20 el 03mar
//   B (de R2, CERRADA)  consumo 3 x 333 = 999 el 20feb  pago 777 el 01abr   (ambos fuera del período)
//
//   ingresos servicios = 50   (el de 999 queda fuera por fechaHora: RN-7)
//   cobrado            = 20   (el de 777 queda fuera por fecha: RN-8)
//   ingresos totales   = 850 + 50 = 900
//   ingreso/huésped    = 900 / 3  = 300
//   saldo pendiente    = 50 - 20  = 30   (solo cuentas ABIERTA: RN-9)
//
//   tasa de cancelación = 1 / 1 = 100 %  (RN-10: por fechaCreacion; en marzo solo se creó R3)
// =============================================================================================
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DashboardServiceIntegrationTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 3, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 3, 10);

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
    private EntityManager entityManager;

    private Cliente cliente;
    private Operador operador;
    private Servicio servicio;

    @BeforeEach
    void setUp() {
        TipoHabitacion tipo = tipoHabitacionRepository.save(
                TipoHabitacion.builder().nombre("Test Suite").build());

        Habitacion t01 = guardarHabitacion("T01", tipo, EstadoHabitacion.DISPONIBLE);
        Habitacion t02 = guardarHabitacion("T02", tipo, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("T03", tipo, EstadoHabitacion.MANTENIMIENTO);
        guardarHabitacion("T04", tipo, EstadoHabitacion.DESHABILITADA);

        cliente = clienteRepository.save(Cliente.builder()
                .nombre("Ana").apellido("Prueba")
                .email("ana.dashboard@email.com").password("pass1234").cedula("DASH-1")
                .build());

        operador = operadorRepository.save(Operador.builder()
                .nombre("Operador Prueba").email("op.dashboard@theroyale.com")
                .password("operator123").tipo(TipoOperador.OPERADOR)
                .build());

        servicio = servicioRepository.save(Servicio.builder()
                .nombre("Spa Test").descripcion("Servicio de prueba").precio(25.0)
                .imagenUrl("/images/test.webp")
                .build());

        Reserva r1 = guardarReserva(t01, "2026-03-01", "2026-03-04", EstadoReserva.FINALIZADA, 100, "2026-02-20");
        Reserva r2 = guardarReserva(t02, "2026-02-27", "2026-03-03", EstadoReserva.CONFIRMADA, 200, "2026-02-20");
        guardarReserva(t01, "2026-03-05", "2026-03-07", EstadoReserva.CANCELADA, 500, "2026-03-05");
        guardarReserva(t02, "2026-03-08", "2026-03-12", EstadoReserva.EN_CURSO, 50, "2026-02-26");
        guardarReserva(t01, "2026-03-15", "2026-03-18", EstadoReserva.CONFIRMADA, 400, "2026-02-25");
        guardarReserva(t02, "2026-03-02", "2026-03-04", EstadoReserva.PENDIENTE, 999, "2026-02-25");

        // Cuenta A: dentro del período y abierta, es la que deja saldo pendiente
        guardarCuenta(r1, EstadoCuenta.ABIERTA,
                2, 25, "2026-03-02T15:00:00",
                20, "2026-03-03T10:00:00", "CARD");

        // Cuenta B: cerrada y con movimientos fuera del período, para comprobar que se filtran
        guardarCuenta(r2, EstadoCuenta.CERRADA,
                3, 333, "2026-02-20T12:00:00",
                777, "2026-04-01T12:00:00", "CASH");

        // Las consultas nativas no disparan el flush automático de Hibernate (las JPQL sí): sin este
        // flush el SQL no vería nada de lo que se acaba de guardar y las sumas saldrían en cero.
        entityManager.flush();
    }

    // ===== Ocupación, ADR y RevPAR: el corazón del panel =====
    @Test
    void calcularKpis_conPeriodoControlado_devuelveOcupacionAdrYRevparCalculadosAMano() {
        // Act
        KpisResponse kpis = dashboardService.resumen(DESDE, HASTA).actual();

        // Assert
        assertThat(kpis.diasPeriodo()).isEqualTo(10);
        assertThat(kpis.habitacionesDisponibles()).isEqualTo(3);        // T04 fuera del inventario
        assertThat(kpis.habitacionesMantenimiento()).isEqualTo(1);      // T03 se informa aparte
        assertThat(kpis.nochesVendidas()).isEqualTo(8);
        assertThat(kpis.ingresosHabitaciones()).isEqualByComparingTo("850");
        assertThat(kpis.ocupacionPorcentaje()).isEqualByComparingTo("26.67");
        assertThat(kpis.adr()).isEqualByComparingTo("106.25");
        assertThat(kpis.revpar()).isEqualByComparingTo("28.33");
    }

    // ===== Facturado y cobrado son cifras distintas y no deben mezclarse (RN-7, RN-8) =====
    @Test
    void calcularKpis_conConsumosYPagos_separaFacturadoDeCobrado() {
        // Act
        KpisResponse kpis = dashboardService.resumen(DESDE, HASTA).actual();

        // Assert
        assertThat(kpis.ingresosServicios()).isEqualByComparingTo("50");   // el consumo de 999 es de febrero
        assertThat(kpis.ingresosTotales()).isEqualByComparingTo("900");    // 850 de habitación + 50 de servicios
        assertThat(kpis.cobrado()).isEqualByComparingTo("20");             // el pago de 777 es de abril
        assertThat(kpis.cobrado()).isNotEqualByComparingTo(kpis.ingresosTotales());
        assertThat(kpis.saldoPendiente()).isEqualByComparingTo("30");      // solo la cuenta abierta
    }

    // ===== Promedios y derivados por reserva (RN-11, RN-12, RN-13) =====
    @Test
    void calcularKpis_conTresReservasVendibles_devuelvePromediosExactos() {
        // Act
        KpisResponse kpis = dashboardService.resumen(DESDE, HASTA).actual();

        // Assert
        assertThat(kpis.reservasAtendidas()).isEqualTo(3);
        assertThat(kpis.alosNoches()).isEqualByComparingTo("3.67");
        assertThat(kpis.leadTimeDias()).isEqualByComparingTo("8.67");
        assertThat(kpis.ingresoPorHuesped()).isEqualByComparingTo("300");
    }

    // ===== PENDIENTE y CANCELADA no son noches vendidas (RN-1) =====
    // Es la regla más fácil de romper: si entraran, la reserva PENDIENTE de 999 y la CANCELADA de
    // 500 dispararían los ingresos y la ocupación sin que el hotel haya vendido nada.
    @Test
    void calcularKpis_conReservasCanceladaYPendiente_noLasCuentaComoVendidas() {
        // Act
        KpisResponse kpis = dashboardService.resumen(DESDE, HASTA).actual();

        // Assert: con las 5 reservas del período contarían 8 + 2 (cancelada) + 2 (pendiente) noches
        assertThat(kpis.nochesVendidas()).isEqualTo(8);
        assertThat(kpis.ingresosHabitaciones()).isEqualByComparingTo("850");

        // La cancelada sí cuenta para la tasa de cancelación, porque se creó dentro del período
        assertThat(kpis.tasaCancelacion()).isEqualByComparingTo("100.00");
    }

    // ===== Las reservas a caballo del borde se recortan, no se cuentan enteras (RN-1, RN-6) =====
    @Test
    void calcularKpis_conReservasQueCruzanElBorde_recortaLasNochesAlPeriodo() {
        // Act: R2 (27feb-03mar) y R4 (08mar-12mar) solo aportan los días dentro del rango
        KpisResponse completo = dashboardService.resumen(DESDE, HASTA).actual();

        // Un solo día, el 1 de marzo: solo R1 y R2 lo ocupan, una noche cada una
        KpisResponse unDia = dashboardService.resumen(DESDE, DESDE).actual();

        // Assert
        assertThat(completo.nochesVendidas()).isEqualTo(8);
        assertThat(unDia.diasPeriodo()).isEqualTo(1);
        assertThat(unDia.nochesVendidas()).isEqualTo(2);
        assertThat(unDia.ingresosHabitaciones()).isEqualByComparingTo("300");  // 100 de R1 + 200 de R2
        assertThat(unDia.ocupacionPorcentaje()).isEqualByComparingTo("66.67"); // 2 de 3 habitaciones
    }

    // ===== Sin reseñas el rating va en null, no en cero (RN-14) =====
    @Test
    void calcularKpis_sinResenasEnElPeriodo_dejaElRatingEnNull() {
        // Act
        KpisResponse kpis = dashboardService.resumen(DESDE, HASTA).actual();

        // Assert: un 0 se leería como "nos calificaron con cero"
        assertThat(kpis.totalResenas()).isZero();
        assertThat(kpis.ratingPromedio()).isNull();
    }

    // ===== El período anterior equivalente (RN-15) =====
    @Test
    void resumen_conPeriodoDeDiezDias_devuelveLosDiezDiasAnterioresComoComparacion() {
        // Act
        ResumenResponse resumen = dashboardService.resumen(DESDE, HASTA);

        // Assert: del 19 al 28 de febrero, justo antes y con la misma longitud
        assertThat(resumen.hastaAnterior()).isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(resumen.desdeAnterior()).isEqualTo(LocalDate.of(2026, 2, 19));
        assertThat(resumen.anterior().diasPeriodo()).isEqualTo(10);

        // En ese período solo R2 estaba en casa (27 y 28 de febrero): 2 noches a 200
        assertThat(resumen.anterior().nochesVendidas()).isEqualTo(2);
        assertThat(resumen.anterior().ingresosHabitaciones()).isEqualByComparingTo("400");
    }

    // ===== La serie diaria cuadra con el total del período =====
    @Test
    void ocupacionSerie_enPeriodoHistorico_devuelveUnPuntoPorDiaQueSumaLasNochesDelPeriodo() {
        // Act
        List<PuntoOcupacionResponse> serie = dashboardService.ocupacionSerie(DESDE, HASTA);

        // Assert: 10 días, ninguno marcado como futuro (marzo de 2026 ya pasó)
        assertThat(serie).hasSize(10);
        assertThat(serie).allMatch(punto -> !punto.futuro());
        assertThat(serie.get(0).fecha()).isEqualTo(DESDE);
        assertThat(serie.get(9).fecha()).isEqualTo(HASTA);

        // Día a día: 1mar y 2mar con R1 y R2; 3mar solo R1 (R2 ya salió); del 4 al 7 vacío; 8, 9 y 10 con R4
        assertThat(serie.stream().map(PuntoOcupacionResponse::nochesVendidas))
                .containsExactly(2L, 2L, 1L, 0L, 0L, 0L, 0L, 1L, 1L, 1L);

        // El total de la serie es el mismo que el del resumen: 8 noches
        assertThat(serie.stream().mapToLong(PuntoOcupacionResponse::nochesVendidas).sum()).isEqualTo(8);

        // Los días sin ninguna reserva salen en cero, no faltan
        assertThat(serie.get(3).ocupacionPorcentaje()).isEqualByComparingTo("0.00");
        assertThat(serie.get(0).ocupacionPorcentaje()).isEqualByComparingTo("66.67");
    }

    // ===== La serie de ingresos reparte cada noche en su día =====
    @Test
    void ingresosSerie_agrupadaPorDia_imputaCadaNocheYCadaPagoASuDia() {
        // Act
        List<PuntoIngresosResponse> serie = dashboardService.ingresosSerie(DESDE, HASTA, "dia");

        // Assert
        assertThat(serie).hasSize(10);

        // 1 de marzo: R1 (100) y R2 (200) ocupan, nada de servicios ni de caja
        assertThat(serie.get(0).habitaciones()).isEqualByComparingTo("300");
        assertThat(serie.get(0).servicios()).isEqualByComparingTo("0");
        assertThat(serie.get(0).cobrado()).isEqualByComparingTo("0");

        // 2 de marzo: el consumo de 50 cae aquí
        assertThat(serie.get(1).servicios()).isEqualByComparingTo("50");

        // 3 de marzo: el pago de 20 cae aquí, y ya solo ocupa R1
        assertThat(serie.get(2).habitaciones()).isEqualByComparingTo("100");
        assertThat(serie.get(2).cobrado()).isEqualByComparingTo("20");

        // La suma de la serie coincide con los KPIs del mismo período
        KpisResponse kpis = dashboardService.resumen(DESDE, HASTA).actual();
        assertThat(serie.stream().map(PuntoIngresosResponse::habitaciones)
                .reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo(kpis.ingresosHabitaciones());
        assertThat(serie.stream().map(PuntoIngresosResponse::servicios)
                .reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo(kpis.ingresosServicios());
        assertThat(serie.stream().map(PuntoIngresosResponse::cobrado)
                .reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo(kpis.cobrado());
    }

    // ===== Agrupar por mes junta los días sin perder importe =====
    @Test
    void ingresosSerie_agrupadaPorMes_devuelveUnPuntoConElTotalDelMes() {
        // Act
        List<PuntoIngresosResponse> serie = dashboardService.ingresosSerie(DESDE, HASTA, "mes");

        // Assert
        assertThat(serie).hasSize(1);
        assertThat(serie.get(0).etiqueta()).isEqualTo("Mar 2026");
        assertThat(serie.get(0).habitaciones()).isEqualByComparingTo("850");
        assertThat(serie.get(0).servicios()).isEqualByComparingTo("50");
    }

    // ===== Desgloses: cada uno suma su propio total y reparte el 100 % =====
    @Test
    void desglose_conUnTipoUnServicioYUnMetodo_repartePorcentajesSobreElTotal() {
        // Act
        DesgloseResponse desglose = dashboardService.desglose(DESDE, HASTA);

        // Assert
        assertThat(desglose.porTipoHabitacion()).hasSize(1);
        assertThat(desglose.porTipoHabitacion().get(0).etiqueta()).isEqualTo("Test Suite");
        assertThat(desglose.porTipoHabitacion().get(0).valor()).isEqualByComparingTo("850");
        assertThat(desglose.porTipoHabitacion().get(0).cantidad()).isEqualTo(8);
        assertThat(desglose.porTipoHabitacion().get(0).porcentaje()).isEqualByComparingTo("100.00");

        assertThat(desglose.porServicio()).hasSize(1);
        assertThat(desglose.porServicio().get(0).etiqueta()).isEqualTo("Spa Test");
        assertThat(desglose.porServicio().get(0).valor()).isEqualByComparingTo("50");
        assertThat(desglose.porServicio().get(0).cantidad()).isEqualTo(2);  // unidades vendidas

        assertThat(desglose.porMetodoPago()).hasSize(1);
        assertThat(desglose.porMetodoPago().get(0).etiqueta()).isEqualTo("CARD");
        assertThat(desglose.porMetodoPago().get(0).valor()).isEqualByComparingTo("20");

        // Desempeño del operador: 50 cargados + 20 cobrados = 70 de actividad en 2 operaciones
        assertThat(desglose.porOperador()).hasSize(1);
        assertThat(desglose.porOperador().get(0).valor()).isEqualByComparingTo("70");
        assertThat(desglose.porOperador().get(0).cantidad()).isEqualTo(2);
    }

    // ===== Operación del día: el mapa deduce el piso del número de habitación =====
    @Test
    void operacionHoy_siempre_devuelveElMapaConTodasLasHabitacionesYSuEstado() {
        // Act
        var hoy = dashboardService.operacionHoy();

        // Assert
        assertThat(hoy.fecha()).isEqualTo(LocalDate.now());
        assertThat(hoy.habitaciones()).hasSize(4);
        assertThat(hoy.habitacionesMantenimiento()).isEqualTo(1);
        assertThat(hoy.habitacionesDeshabilitadas()).isEqualTo(1);

        // "T01" tiene 3 caracteres: el piso es lo que queda al quitar los dos últimos
        assertThat(hoy.habitaciones().get(0).numero()).isEqualTo("T01");
        assertThat(hoy.habitaciones().get(0).piso()).isEqualTo("T");

        // El saldo pendiente global no depende de la fecha de salida: la cuenta A sigue debiendo 30
        assertThat(hoy.saldoPendienteTotal()).isEqualByComparingTo("30");
    }

    // ===== Reputación sin reseñas: estructura completa y sin inventar un rating =====
    @Test
    void reputacion_sinResenas_devuelveLasCincoEstrellasEnCeroYPromedioNull() {
        // Act
        var reputacion = dashboardService.reputacion(DESDE, HASTA);

        // Assert
        assertThat(reputacion.total()).isZero();
        assertThat(reputacion.promedio()).isNull();
        assertThat(reputacion.distribucion()).hasSize(5).containsOnlyKeys(1, 2, 3, 4, 5);
        assertThat(reputacion.distribucion().values()).allMatch(conteo -> conteo == 0L);

        // Las 3 reservas vendibles son del mismo cliente: la primera es nueva y las otras recurrentes
        assertThat(reputacion.reservasClientesNuevos() + reputacion.reservasClientesRecurrentes())
                .isEqualTo(3);
    }

    // ===== Períodos sin actividad: ceros, no divisiones por cero =====
    // Es el estado real de un hotel recién abierto y el de cualquier rango sin reservas. Si alguna
    // división no estuviera protegida, el panel entero respondería 500 en lugar de mostrar ceros.
    @Test
    void calcularKpis_enPeriodoSinNingunaReserva_devuelveCerosSinRomperse() {
        // Act: enero de 2026, antes de cualquier dato del dataset
        KpisResponse kpis = dashboardService.resumen(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)).actual();

        // Assert
        assertThat(kpis.nochesVendidas()).isZero();
        assertThat(kpis.reservasAtendidas()).isZero();
        assertThat(kpis.ingresosHabitaciones()).isEqualByComparingTo("0");
        assertThat(kpis.ingresosServicios()).isEqualByComparingTo("0");
        assertThat(kpis.cobrado()).isEqualByComparingTo("0");
        assertThat(kpis.ocupacionPorcentaje()).isEqualByComparingTo("0");  // 0 / (3 x 31)
        assertThat(kpis.adr()).isEqualByComparingTo("0");                  // 0 noches: no se divide
        assertThat(kpis.revpar()).isEqualByComparingTo("0");
        assertThat(kpis.ingresoPorHuesped()).isEqualByComparingTo("0");    // 0 reservas: no se divide
        assertThat(kpis.tasaCancelacion()).isEqualByComparingTo("0");      // 0 creadas: no se divide
        assertThat(kpis.alosNoches()).isEqualByComparingTo("0");
        assertThat(kpis.leadTimeDias()).isEqualByComparingTo("0");
        assertThat(kpis.ratingPromedio()).isNull();
    }

    // ===== Validación del período =====
    @Test
    void resumen_conFechasInvertidas_lanzaIllegalArgumentException() {
        // Act & Assert: ApiExceptionHandler lo traduce a 400
        assertThatThrownBy(() -> dashboardService.resumen(HASTA, DESDE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("desde");
    }

    @Test
    void resumen_conPeriodoMayorAUnAnio_lanzaIllegalArgumentException() {
        // Act & Assert
        assertThatThrownBy(() -> dashboardService.resumen(DESDE, DESDE.plusYears(2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("366");
    }

    @Test
    void ingresosSerie_conAgrupacionDesconocida_lanzaIllegalArgumentException() {
        // Act & Assert
        assertThatThrownBy(() -> dashboardService.ingresosSerie(DESDE, HASTA, "trimestre"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dia, semana or mes");
    }

    // ===== Utilidades de armado =====

    private Habitacion guardarHabitacion(String numero, TipoHabitacion tipo, EstadoHabitacion estado) {
        return habitacionRepository.save(Habitacion.builder()
                .numero(numero).tipoHabitacion(tipo).precio(100.0).estado(estado)
                .build());
    }

    private Reserva guardarReserva(Habitacion habitacion, String inicio, String fin,
                                   EstadoReserva estado, int precioNoche, String creada) {
        return reservaRepository.save(Reserva.builder()
                .cliente(cliente)
                .habitacion(habitacion)
                .fechaInicio(LocalDate.parse(inicio))
                .fechaFin(LocalDate.parse(fin))
                .estado(estado)
                .cantidadPersonas(2)
                .precioNocheAcordado(BigDecimal.valueOf(precioNoche))
                .fechaCreacion(LocalDate.parse(creada).atTime(9, 0))
                .build());
    }

    private void guardarCuenta(Reserva reserva, EstadoCuenta estado,
                               int cantidad, int precioUnitario, String fechaHoraConsumo,
                               int montoPago, String fechaPago, String metodoPago) {
        Cuenta cuenta = Cuenta.builder()
                .reserva(reserva)
                .estado(estado)
                .fechaCreacion(reserva.getFechaInicio().atTime(15, 0))
                .itemsConsumo(new ArrayList<>())
                .pagos(new ArrayList<>())
                .build();

        cuenta.getItemsConsumo().add(ItemConsumo.builder()
                .cuenta(cuenta)
                .servicio(servicio)
                .operador(operador)
                .cantidad(cantidad)
                .precioUnitario(BigDecimal.valueOf(precioUnitario))
                .fechaHora(LocalDateTime.parse(fechaHoraConsumo))
                .pagado(false)
                .build());

        cuenta.getPagos().add(Pago.builder()
                .cuenta(cuenta)
                .operador(operador)
                .monto(BigDecimal.valueOf(montoPago))
                .fecha(LocalDateTime.parse(fechaPago))
                .metodoPago(metodoPago)
                .build());

        cuentaRepository.save(cuenta);
    }
}
