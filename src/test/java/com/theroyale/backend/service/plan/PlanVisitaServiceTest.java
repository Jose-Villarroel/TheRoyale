package com.theroyale.backend.service.plan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.theroyale.backend.config.GeminiProperties;
import com.theroyale.backend.config.HotelProperties;
import com.theroyale.backend.config.PlanVisitaProperties;
import com.theroyale.backend.dto.CuentaResponse;
import com.theroyale.backend.dto.plan.AgregarBloqueRequest;
import com.theroyale.backend.dto.plan.AjustarPlanRequest;
import com.theroyale.backend.dto.plan.BloquePlanResponse;
import com.theroyale.backend.dto.plan.DiaPlanResponse;
import com.theroyale.backend.dto.plan.EntendidoItem;
import com.theroyale.backend.dto.plan.GenerarPlanRequest;
import com.theroyale.backend.dto.plan.InterpretarPlanRequest;
import com.theroyale.backend.dto.plan.PlanVisitaResponse;
import com.theroyale.backend.dto.plan.PreferenciasPlan;
import com.theroyale.backend.dto.plan.RegenerarDiaRequest;
import com.theroyale.backend.errors.AccesoDenegadoException;
import com.theroyale.backend.errors.LimiteExcedidoException;
import com.theroyale.backend.errors.RecursoNoEncontradoException;
import com.theroyale.backend.errors.ServicioNoDisponibleException;
import com.theroyale.backend.model.CategoriaLugar;
import com.theroyale.backend.model.Cliente;
import com.theroyale.backend.model.EstadoReserva;
import com.theroyale.backend.model.EtiquetaPlato;
import com.theroyale.backend.model.FuentePlan;
import com.theroyale.backend.model.Habitacion;
import com.theroyale.backend.model.Interes;
import com.theroyale.backend.model.PlanVisita;
import com.theroyale.backend.model.Presupuesto;
import com.theroyale.backend.model.Reserva;
import com.theroyale.backend.model.RestauranteHotel;
import com.theroyale.backend.model.Ritmo;
import com.theroyale.backend.model.TipoBloque;
import com.theroyale.backend.model.TipoClima;
import com.theroyale.backend.model.TipoHabitacion;
import com.theroyale.backend.repository.PlanVisitaRepository;
import com.theroyale.backend.repository.ReservaRepository;
import com.theroyale.backend.service.CuentaService;
import com.theroyale.backend.service.chat.GeminiClient;
import com.theroyale.backend.service.chat.LimiteMensajesService;
import com.theroyale.backend.service.plan.ContextoPlan.LugarContexto;
import com.theroyale.backend.service.plan.ContextoPlan.RestauranteContexto;
import com.theroyale.backend.service.plan.ContextoPlan.ServicioContexto;
import com.theroyale.backend.service.plan.PlanGenerado.BloqueGenerado;
import com.theroyale.backend.service.plan.PlanGenerado.DiaGenerado;
import com.theroyale.backend.service.plan.fuentes.ClimaDia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// ===== Pruebas de PlanVisitaService: reglas de la reserva, límite diario, regenerar, ajustar, =====
// "Add to my stay", privacidad y tarea de clima. Repositorios, contexto y cuenta se simulan con Mockito;
// el validador y el generador son los reales, y Gemini es un falso que guarda cada solicitud.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlanVisitaServiceTest {

    private static final ZoneId NUEVA_YORK = ZoneId.of("America/New_York");
    private static final Clock HOY = Clock.fixed(LocalDateTime.of(2026, 11, 11, 10, 0).atZone(NUEVA_YORK).toInstant(), NUEVA_YORK);
    private static final LocalDate LLEGADA = LocalDate.of(2026, 12, 12);
    private static final LocalDate SALIDA = LocalDate.of(2026, 12, 15);
    private static final String MASAJE = "Signature Massage - 60 or 90 min, Swedish, deep tissue and hot stone";

    @Mock
    private PlanVisitaRepository planVisitaRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private ContextoPlanService contextoPlanService;

    @Mock
    private InterpretePlanService interpretePlanService;

    @Mock
    private CuentaService cuentaService;

    @Mock
    private LimiteMensajesService limiteMensajesService;

    @Spy
    private ValidadorPlanService validadorPlanService = new ValidadorPlanService();

    @Spy
    private GeneradorPlanService generadorPlanService = new GeneradorPlanService();

    @Spy
    private GeminiProperties geminiProperties = new GeminiProperties();

    @Spy
    private PlanVisitaProperties propiedades = new PlanVisitaProperties();

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @InjectMocks
    private PlanVisitaService planVisitaService;

    private final GeminiFalso gemini = new GeminiFalso();
    private Cliente laura;
    private Reserva reserva;

    @BeforeEach
    void setUp() {
        planVisitaService.setReloj(HOY);
        geminiProperties.setApiKey("clave-de-prueba");
        ReflectionTestUtils.setField(generadorPlanService, "geminiClient", gemini);
        ReflectionTestUtils.setField(generadorPlanService, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(generadorPlanService, "hotel", new HotelProperties());

        laura = Cliente.builder().id(8L).nombre("Laura").apellido("Martinez").email("laura.martinez@email.com")
                .cedula("1098765432").telefono("+1-555-0199").build();
        reserva = reserva(12L, laura, EstadoReserva.CONFIRMADA, LLEGADA, SALIDA);
        when(reservaRepository.findById(12L)).thenReturn(Optional.of(reserva));
        when(planVisitaRepository.findByReservaId(12L)).thenReturn(Optional.empty());
        when(planVisitaRepository.save(any(PlanVisita.class))).thenAnswer(i -> i.getArgument(0));
        when(contextoPlanService.construir(anyList(), any())).thenAnswer(i -> contexto(i.getArgument(0)));
    }

    // ===== Generar =====
    @Test
    void planVisitaService_generar_reservaConfirmada_guardaElPlanValidadoYResponde() throws Exception {
        // Arrange
        gemini.responder(planCompleto());

        // Act
        PlanVisitaResponse respuesta = planVisitaService.generar(12L, solicitud(preferencias()));

        // Assert
        assertThat(respuesta.reservaId()).isEqualTo(12L);
        assertThat(respuesta.estadoReserva()).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(respuesta.noches()).isEqualTo(3);
        assertThat(respuesta.tipoHabitacionNombre()).isEqualTo("Luxury");
        assertThat(respuesta.cantidadPersonas()).isEqualTo(2);
        assertThat(respuesta.dias()).extracting(DiaPlanResponse::fecha)
                .containsExactly(LLEGADA, LLEGADA.plusDays(1), LLEGADA.plusDays(2), SALIDA);
        assertThat(respuesta.fuentesUsadas()).containsExactly(FuentePlan.OPEN_METEO, FuentePlan.NYC_OPEN_DATA);
        assertThat(respuesta.fuentesNoDisponibles()).containsExactly(FuentePlan.TICKETMASTER);
        assertThat(respuesta.generacionesRestantesHoy()).isEqualTo(4);
        assertThat(respuesta.entendido()).extracting(EntendidoItem::titulo).containsExactly("An anniversary.");
        assertThat(respuesta.descripcion()).isEqualTo("Our anniversary trip.");
        assertThat(respuesta.fechaGeneracion()).isEqualTo(LocalDateTime.of(2026, 11, 11, 10, 0));
        // "Add to my stay" disponible porque la reserva está CONFIRMADA (cuenta abierta)
        BloquePlanResponse masaje = bloque(respuesta, TipoBloque.SERVICIO_HOTEL);
        assertThat(masaje.agregable()).isTrue();
        assertThat(masaje.precio()).isEqualTo(45.0);

        ArgumentCaptor<PlanVisita> guardado = ArgumentCaptor.forClass(PlanVisita.class);
        verify(planVisitaRepository).save(guardado.capture());
        assertThat(guardado.getValue().getGeneracionesHoy()).isEqualTo(1);
        assertThat(guardado.getValue().getFechaUltimaGeneracion()).isEqualTo(LocalDate.of(2026, 11, 11));
        assertThat(guardado.getValue().getModeloIa()).isEqualTo("modelo-falso");
        assertThat(guardado.getValue().getFuentesUsadas()).isEqualTo("OPEN_METEO,NYC_OPEN_DATA");
    }

    @Test
    void planVisitaService_generar_reservaPendiente_elServicioQuedaNoAgregable() throws Exception {
        // Arrange
        reserva.setEstado(EstadoReserva.PENDIENTE);
        gemini.responder(planCompleto());

        // Act
        PlanVisitaResponse respuesta = planVisitaService.generar(12L, solicitud(preferencias()));

        // Assert
        assertThat(bloque(respuesta, TipoBloque.SERVICIO_HOTEL).agregable()).isFalse();
    }

    @Test
    void planVisitaService_generar_privacidad_aGeminiNoLlegaNingunDatoPersonal() throws Exception {
        // Arrange
        gemini.responder(planCompleto());

        // Act
        planVisitaService.generar(12L, solicitud(preferencias()));

        // Assert: se captura lo que recibió el Gemini falso (criterio de aceptación 11)
        String enviado = gemini.ultimaSolicitud();
        assertThat(enviado).contains("2026-12-12").contains("\"huespedes\":2").contains("MUSEOS");
        assertThat(enviado).doesNotContain("Laura").doesNotContain("Martinez")
                .doesNotContain("laura.martinez@email.com").doesNotContain("1098765432").doesNotContain("555-0199");
    }

    @Test
    void planVisitaService_generar_reservaAjena_lanzaAccesoDenegadoSinLlamarAGemini() {
        // Arrange
        GenerarPlanRequest ajena = new GenerarPlanRequest(99L, preferencias(), null, null);

        // Act & Assert
        assertThatThrownBy(() -> planVisitaService.generar(12L, ajena))
                .isInstanceOf(AccesoDenegadoException.class)
                .hasMessage("This reservation does not belong to you.");
        assertThat(gemini.solicitudes).isEmpty();
    }

    @Test
    void planVisitaService_generar_reservaInexistente_lanzaRecursoNoEncontrado() {
        // Arrange
        when(reservaRepository.findById(404L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> planVisitaService.generar(404L, solicitud(preferencias())))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoReserva.class, names = {"CANCELADA", "FINALIZADA"})
    void planVisitaService_generar_estadoNoPlanificable_lanzaIllegalState(EstadoReserva estado) {
        // Arrange
        reserva.setEstado(estado);

        // Act & Assert
        assertThatThrownBy(() -> planVisitaService.generar(12L, solicitud(preferencias())))
                .isInstanceOf(IllegalStateException.class);
        assertThat(gemini.solicitudes).isEmpty();
    }

    @Test
    void planVisitaService_generar_masDe14Noches_lanzaIllegalArgument() {
        // Arrange
        reserva.setFechaFin(LLEGADA.plusDays(15));

        // Act & Assert
        assertThatThrownBy(() -> planVisitaService.generar(12L, solicitud(preferencias())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("14 nights");
    }

    @Test
    void planVisitaService_generar_14NochesExactas_sePermite() throws Exception {
        // Arrange
        reserva.setFechaFin(LLEGADA.plusDays(14));
        gemini.responder(planCompleto());

        // Act
        PlanVisitaResponse respuesta = planVisitaService.generar(12L, solicitud(preferencias()));

        // Assert: 15 días; los que la IA no devolvió quedan como tiempo libre
        assertThat(respuesta.dias()).hasSize(15);
    }

    static Stream<PreferenciasPlan> preferenciasInvalidas() {
        return Stream.of(
                new PreferenciasPlan(List.of(), Ritmo.EQUILIBRADO, Presupuesto.ALTO, List.of(), false, false, null),
                new PreferenciasPlan(List.of(Interes.MUSEOS, Interes.TEATRO, Interes.PARQUES, Interes.COMPRAS,
                        Interes.FAMILIA, Interes.BIENESTAR), Ritmo.EQUILIBRADO, Presupuesto.ALTO, List.of(), false, false, null),
                new PreferenciasPlan(List.of(Interes.MUSEOS, Interes.MUSEOS), Ritmo.EQUILIBRADO, Presupuesto.ALTO, List.of(), false, false, null),
                new PreferenciasPlan(List.of(Interes.MUSEOS), null, Presupuesto.ALTO, List.of(), false, false, null),
                new PreferenciasPlan(List.of(Interes.MUSEOS), Ritmo.EQUILIBRADO, null, List.of(), false, false, null),
                new PreferenciasPlan(List.of(Interes.MUSEOS), Ritmo.EQUILIBRADO, Presupuesto.ALTO,
                        List.of(EtiquetaPlato.PICANTE), false, false, null),
                new PreferenciasPlan(List.of(Interes.MUSEOS), Ritmo.EQUILIBRADO, Presupuesto.ALTO, List.of(), false, false,
                        "x".repeat(301)));
    }

    @ParameterizedTest
    @MethodSource("preferenciasInvalidas")
    void planVisitaService_generar_preferenciasInvalidas_lanzaIllegalArgument(PreferenciasPlan invalidas) {
        // Act & Assert
        assertThatThrownBy(() -> planVisitaService.generar(12L, solicitud(invalidas)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(gemini.solicitudes).isEmpty();
    }

    @Test
    void planVisitaService_generar_descripcionDeMasDe1000_lanzaIllegalArgument() {
        // Arrange
        GenerarPlanRequest larga = new GenerarPlanRequest(8L, preferencias(), "x".repeat(1001), null);

        // Act & Assert
        assertThatThrownBy(() -> planVisitaService.generar(12L, larga)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void planVisitaService_generar_sinClaveDeGemini_lanzaServicioNoDisponible() {
        // Arrange
        geminiProperties.setApiKey("");

        // Act & Assert
        assertThatThrownBy(() -> planVisitaService.generar(12L, solicitud(preferencias())))
                .isInstanceOf(ServicioNoDisponibleException.class)
                .hasMessage("The planner is unavailable right now.");
        assertThat(planVisitaService.estaDisponible()).isFalse();
        verify(contextoPlanService, never()).construir(anyList(), any());
    }

    @Test
    void planVisitaService_generar_enCurso_soloPlanificaLosDiasQueFaltan() throws Exception {
        // Arrange: llegó el 9 y sale el 13; hoy es 11
        reserva.setEstado(EstadoReserva.EN_CURSO);
        reserva.setFechaInicio(LocalDate.of(2026, 11, 9));
        reserva.setFechaFin(LocalDate.of(2026, 11, 13));
        gemini.responder(new PlanGenerado("r", List.of(
                new DiaGenerado("2026-11-11", "Today", List.of(
                        new BloqueGenerado("10:00", "12:00", "LUGAR", "1", null, "MoMA", "Art."))),
                new DiaGenerado("2026-11-13", "Last morning", List.of(
                        new BloqueGenerado("09:00", "11:00", "LUGAR", "2", null, "Central Park", "Walk."))))));

        // Act
        PlanVisitaResponse respuesta = planVisitaService.generar(12L, solicitud(preferencias()));

        // Assert: sin nota de check-in (el día de llegada ya pasó)
        assertThat(respuesta.dias()).extracting(DiaPlanResponse::fecha)
                .containsExactly(LocalDate.of(2026, 11, 11), LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 13));
        assertThat(respuesta.dias().get(0).numero()).isEqualTo(3);
        assertThat(respuesta.dias().get(0).tipoDia()).isEqualTo("COMPLETO");
    }

    // ===== Límite de 5 generaciones por día =====
    @Test
    void planVisitaService_generar_sextaGeneracionDelDia_lanzaLimiteExcedido() {
        // Arrange
        PlanVisita plan = planGuardado(5, LocalDate.of(2026, 11, 11), List.of());
        when(planVisitaRepository.findByReservaId(12L)).thenReturn(Optional.of(plan));

        // Act & Assert
        assertThatThrownBy(() -> planVisitaService.generar(12L, solicitud(preferencias())))
                .isInstanceOf(LimiteExcedidoException.class)
                .hasMessage("You have reached today's limit of 5 plans for this stay. Try again tomorrow.");
        assertThatThrownBy(() -> planVisitaService.ajustar(12L, new AjustarPlanRequest(8L, "More museums")))
                .isInstanceOf(LimiteExcedidoException.class);
        assertThatThrownBy(() -> planVisitaService.regenerarDia(12L, LLEGADA.plusDays(1), new RegenerarDiaRequest(8L, null)))
                .isInstanceOf(LimiteExcedidoException.class);
        assertThat(gemini.solicitudes).isEmpty();
    }

    @Test
    void planVisitaService_generar_limiteDeAyer_seReiniciaHoy() throws Exception {
        // Arrange
        PlanVisita plan = planGuardado(5, LocalDate.of(2026, 11, 10), List.of());
        when(planVisitaRepository.findByReservaId(12L)).thenReturn(Optional.of(plan));
        gemini.responder(planCompleto());

        // Act
        PlanVisitaResponse respuesta = planVisitaService.generar(12L, solicitud(preferencias()));

        // Assert: reemplaza el plan existente y cuenta 1 generación hoy
        assertThat(plan.getGeneracionesHoy()).isEqualTo(1);
        assertThat(respuesta.generacionesRestantesHoy()).isEqualTo(4);
    }

    // ===== Regenerar un día =====
    @Test
    void planVisitaService_regenerarDia_cambiaSoloEseDiaYLoSacaDeRevision() throws Exception {
        // Arrange
        LocalDate dia = LLEGADA.plusDays(1);
        PlanVisita plan = planGuardado(1, LocalDate.of(2026, 11, 11), List.of(dia));
        when(planVisitaRepository.findByReservaId(12L)).thenReturn(Optional.of(plan));
        List<DiaPlanResponse> antes = leer(plan).dias();
        gemini.responder(new PlanGenerado("otro", List.of(new DiaGenerado(dia.toString(), "Something quieter", List.of(
                new BloqueGenerado("11:00", "13:00", "LUGAR", "1", null, "MoMA", "A calm museum morning."))))));

        // Act
        PlanVisitaResponse respuesta = planVisitaService.regenerarDia(12L, dia,
                new RegenerarDiaRequest(8L, "something quieter"));

        // Assert
        assertThat(respuesta.dias()).hasSize(4);
        assertThat(respuesta.dias().get(1).titulo()).isEqualTo("Something quieter");
        assertThat(respuesta.dias().get(1).bloques()).extracting(BloquePlanResponse::motivo)
                .containsExactly("A calm museum morning.");
        // Los otros días quedan idénticos (mismos ids de bloque)
        for (int i : new int[]{0, 2, 3}) {
            assertThat(respuesta.dias().get(i).bloques()).extracting(BloquePlanResponse::id)
                    .containsExactlyElementsOf(antes.get(i).bloques().stream().map(BloquePlanResponse::id).toList());
        }
        assertThat(respuesta.resumen()).isEqualTo("Original summary");
        assertThat(respuesta.diasEnRevision()).isEmpty();
        assertThat(respuesta.requiereRevision()).isFalse();
        assertThat(plan.getGeneracionesHoy()).isEqualTo(2);
        // A Gemini se le pide solo ese día, con la indicación y lo planeado en los otros días
        String enviado = gemini.ultimaSolicitud();
        assertThat(enviado).contains("something quieter").contains("otrosDias")
                .contains("\"diasAPlanificar\":[{\"fecha\":\"2026-12-13\"");
        verify(contextoPlanService).construir(eq(List.of(dia)), any());
    }

    @Test
    void planVisitaService_regenerarDia_fechaFueraDelPlanOIndicacionLarga_lanzaIllegalArgument() {
        // Arrange
        PlanVisita plan = planGuardado(1, LocalDate.of(2026, 11, 11), List.of());
        when(planVisitaRepository.findByReservaId(12L)).thenReturn(Optional.of(plan));

        // Act & Assert
        assertThatThrownBy(() -> planVisitaService.regenerarDia(12L, LocalDate.of(2026, 12, 20), new RegenerarDiaRequest(8L, null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> planVisitaService.regenerarDia(12L, LLEGADA, new RegenerarDiaRequest(8L, "x".repeat(201))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void planVisitaService_regenerarDia_sinPlan_lanzaRecursoNoEncontrado() {
        // Act & Assert
        assertThatThrownBy(() -> planVisitaService.regenerarDia(12L, LLEGADA, new RegenerarDiaRequest(8L, null)))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No plan yet for this reservation.");
    }

    // ===== Ajustar el plan entero =====
    @Test
    void planVisitaService_ajustar_rehaceElPlanConElPlanActualComoContexto() throws Exception {
        // Arrange
        PlanVisita plan = planGuardado(2, LocalDate.of(2026, 11, 11), List.of());
        when(planVisitaRepository.findByReservaId(12L)).thenReturn(Optional.of(plan));
        gemini.responder(planCompleto());

        // Act
        PlanVisitaResponse respuesta = planVisitaService.ajustar(12L, new AjustarPlanRequest(8L, "More time to rest"));

        // Assert
        assertThat(respuesta.dias()).hasSize(4);
        assertThat(respuesta.entendido()).extracting(EntendidoItem::titulo).containsExactly("Saved understanding.");
        assertThat(respuesta.generacionesRestantesHoy()).isEqualTo(2);
        String enviado = gemini.ultimaSolicitud();
        assertThat(enviado).contains("More time to rest").contains("planActual").contains("LUGAR The Museum of Modern Art");
    }

    @Test
    void planVisitaService_ajustar_indicacionVaciaOLarga_lanzaIllegalArgument() {
        // Act & Assert
        assertThatThrownBy(() -> planVisitaService.ajustar(12L, new AjustarPlanRequest(8L, "  ")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> planVisitaService.ajustar(12L, new AjustarPlanRequest(8L, "x".repeat(201))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ===== Add to my stay =====
    @Test
    void planVisitaService_agregarACuenta_servicioDelHotel_cargaCantidad1ConOperadorPredeterminado() throws Exception {
        // Arrange
        PlanVisita plan = planGuardado(1, LocalDate.of(2026, 11, 11), List.of());
        when(planVisitaRepository.findByReservaId(12L)).thenReturn(Optional.of(plan));
        BloquePlanResponse masaje = leer(plan).dias().stream().flatMap(d -> d.bloques().stream())
                .filter(b -> b.tipo() == TipoBloque.SERVICIO_HOTEL).findFirst().orElseThrow();
        CuentaResponse cuenta = new CuentaResponse(1L, 12L, null, null, List.of(), List.of(),
                BigDecimal.valueOf(45), BigDecimal.ZERO, BigDecimal.valueOf(45));
        when(cuentaService.agregarConsumo(12L, 1L, 1, null)).thenReturn(cuenta);

        // Act
        CuentaResponse respuesta = planVisitaService.agregarACuenta(12L, masaje.id(), new AgregarBloqueRequest(8L));

        // Assert
        assertThat(respuesta).isSameAs(cuenta);
        verify(cuentaService).agregarConsumo(12L, 1L, 1, null);
    }

    @Test
    void planVisitaService_agregarACuenta_cuentaNoAbierta_propagaIllegalState() throws Exception {
        // Arrange: reserva PENDIENTE, sin cuenta (CuentaService responde 409)
        reserva.setEstado(EstadoReserva.PENDIENTE);
        PlanVisita plan = planGuardado(1, LocalDate.of(2026, 11, 11), List.of());
        when(planVisitaRepository.findByReservaId(12L)).thenReturn(Optional.of(plan));
        String masajeId = leer(plan).dias().stream().flatMap(d -> d.bloques().stream())
                .filter(b -> b.tipo() == TipoBloque.SERVICIO_HOTEL).findFirst().orElseThrow().id();
        when(cuentaService.agregarConsumo(12L, 1L, 1, null))
                .thenThrow(new IllegalStateException("This reservation does not have an account yet: confirm it first."));

        // Act & Assert
        assertThatThrownBy(() -> planVisitaService.agregarACuenta(12L, masajeId, new AgregarBloqueRequest(8L)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void planVisitaService_agregarACuenta_bloqueQueNoEsServicioOInexistente() throws Exception {
        // Arrange
        PlanVisita plan = planGuardado(1, LocalDate.of(2026, 11, 11), List.of());
        when(planVisitaRepository.findByReservaId(12L)).thenReturn(Optional.of(plan));
        String lugarId = leer(plan).dias().stream().flatMap(d -> d.bloques().stream())
                .filter(b -> b.tipo() == TipoBloque.LUGAR).findFirst().orElseThrow().id();

        // Act & Assert
        assertThatThrownBy(() -> planVisitaService.agregarACuenta(12L, lugarId, new AgregarBloqueRequest(8L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only hotel services can be added to your stay.");
        assertThatThrownBy(() -> planVisitaService.agregarACuenta(12L, "no-existe", new AgregarBloqueRequest(8L)))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> planVisitaService.agregarACuenta(12L, lugarId, new AgregarBloqueRequest(99L)))
                .isInstanceOf(AccesoDenegadoException.class);
        verify(cuentaService, never()).agregarConsumo(any(), any(), any(), any());
    }

    // ===== Obtener, eliminar e interpretar =====
    @Test
    void planVisitaService_obtener_sinPlanOReservaAjena() {
        // Act & Assert
        assertThatThrownBy(() -> planVisitaService.obtener(12L, 8L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No plan yet for this reservation.");
        assertThatThrownBy(() -> planVisitaService.obtener(12L, 99L)).isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    void planVisitaService_eliminar_borraElPlanDeLaReserva() throws Exception {
        // Arrange
        PlanVisita plan = planGuardado(1, LocalDate.of(2026, 11, 11), List.of());
        when(planVisitaRepository.findByReservaId(12L)).thenReturn(Optional.of(plan));

        // Act
        planVisitaService.eliminar(12L, 8L);

        // Assert
        verify(planVisitaRepository).delete(plan);
    }

    @Test
    void planVisitaService_interpretar_registraLaIpYValidaLaDescripcion() {
        // Arrange
        InterpretarPlanRequest valida = new InterpretarPlanRequest(8L, "  Our anniversary, slow mornings.  ");

        // Act
        planVisitaService.interpretar(12L, valida, "10.0.0.1");

        // Assert
        verify(limiteMensajesService).registrar("10.0.0.1");
        verify(interpretePlanService).interpretar(eq("Our anniversary, slow mornings."), any(DatosEstadia.class));
        assertThatThrownBy(() -> planVisitaService.interpretar(12L, new InterpretarPlanRequest(8L, "short"), "10.0.0.1"))
                .isInstanceOf(IllegalArgumentException.class);
        geminiProperties.setApiKey("");
        assertThatThrownBy(() -> planVisitaService.interpretar(12L, valida, "10.0.0.1"))
                .isInstanceOf(ServicioNoDisponibleException.class);
    }

    // ===== Tarea diaria de clima =====
    @Test
    void planVisitaService_actualizarClima_diaAlAireLibreQuePasaALluvia_marcaRevision() throws Exception {
        // Arrange: estadía que empieza mañana; el día 2 tiene Central Park (al aire libre)
        reserva.setFechaInicio(LocalDate.of(2026, 11, 12));
        reserva.setFechaFin(LocalDate.of(2026, 11, 15));
        PlanVisita plan = planGuardado(1, LocalDate.of(2026, 11, 10), List.of());
        when(planVisitaRepository.buscarConDiasEntre(eq(LocalDate.of(2026, 11, 11)), eq(LocalDate.of(2026, 11, 17)), anyList()))
                .thenReturn(List.of(plan));
        Map<LocalDate, ClimaDia> pronostico = new LinkedHashMap<>();
        for (LocalDate d = LocalDate.of(2026, 11, 12); !d.isAfter(LocalDate.of(2026, 11, 15)); d = d.plusDays(1)) {
            int lluvia = d.equals(LocalDate.of(2026, 11, 13)) || d.equals(LocalDate.of(2026, 11, 14)) ? 70 : 10;
            pronostico.put(d, new ClimaDia(d, lluvia >= 60 ? "Rain" : "Sunny", 12, 6, lluvia, TipoClima.PRONOSTICO_NWS));
        }
        when(contextoPlanService.pronosticoNws(LocalDate.of(2026, 11, 11), LocalDate.of(2026, 11, 17))).thenReturn(pronostico);

        // Act
        planVisitaService.actualizarClima();

        // Assert: el 13 tiene un parque y pasa a 70 %; el 14 también llueve pero solo tiene un museo
        assertThat(plan.isRequiereRevision()).isTrue();
        ContenidoPlan contenido = leer(plan);
        assertThat(contenido.diasEnRevision()).containsExactly(LocalDate.of(2026, 11, 13));
        assertThat(contenido.dias()).allMatch(d -> d.clima().tipo() == TipoClima.PRONOSTICO_NWS);
        assertThat(contenido.dias().get(1).clima().probLluvia()).isEqualTo(70);
        verify(planVisitaRepository).save(plan);
        // No regenera nada: Gemini no se llama
        assertThat(gemini.solicitudes).isEmpty();
    }

    @Test
    void planVisitaService_actualizarClima_nwsCaido_noCambiaNada() throws Exception {
        // Arrange
        PlanVisita plan = planGuardado(1, LocalDate.of(2026, 11, 10), List.of());
        when(planVisitaRepository.buscarConDiasEntre(any(), any(), anyList())).thenReturn(List.of(plan));
        when(contextoPlanService.pronosticoNws(any(), any())).thenThrow(new RuntimeException("NWS caído"));

        // Act
        planVisitaService.actualizarClima();

        // Assert
        verify(planVisitaRepository, never()).save(any());
    }

    // ===== Ayudas =====
    private GenerarPlanRequest solicitud(PreferenciasPlan preferencias) {
        return new GenerarPlanRequest(8L, preferencias, "Our anniversary trip.",
                List.of(new EntendidoItem("An anniversary.", "Dinner at the Signature Restaurant.")));
    }

    private static PreferenciasPlan preferencias() {
        return new PreferenciasPlan(List.of(Interes.MUSEOS, Interes.BIENESTAR), Ritmo.EQUILIBRADO, Presupuesto.ALTO,
                List.of(), false, false, "Anniversary");
    }

    // Plan como lo devolvería Gemini para la estadía del 12 al 15 de diciembre
    private PlanGenerado planCompleto() {
        LocalDate inicio = reserva.getFechaInicio();
        return new PlanGenerado("Original summary", List.of(
                new DiaGenerado(inicio.toString(), "Arrival", List.of(
                        new BloqueGenerado("16:00", "17:00", "LUGAR", "1", null, "MoMA", "Close by."))),
                new DiaGenerado(inicio.plusDays(1).toString(), "Park and spa", List.of(
                        new BloqueGenerado("10:00", "12:00", "LUGAR", "2", null, "Central Park", "A walk."),
                        new BloqueGenerado("16:00", "17:30", "SERVICIO_HOTEL", "1", MASAJE, "Massage", "Rest."))),
                new DiaGenerado(inicio.plusDays(2).toString(), "Museum", List.of(
                        new BloqueGenerado("10:00", "12:30", "LUGAR", "1", null, "MoMA", "Art."),
                        new BloqueGenerado("19:00", "21:00", "RESTAURANTE_HOTEL", "SIGNATURE_RESTAURANT", null, "Dinner", "Anniversary."))),
                new DiaGenerado(inicio.plusDays(3).toString(), "Departure", List.of(
                        new BloqueGenerado("09:00", "10:00", "RESTAURANTE_HOTEL", "BREAKFAST_LOUNGE", null, "Breakfast", "Last one.")))));
    }

    // Plan ya guardado (validado con el validador real) para las pruebas de regenerar, ajustar, cuenta y clima
    private PlanVisita planGuardado(int generacionesHoy, LocalDate ultima, List<LocalDate> enRevision) {
        DatosEstadia estadia = DatosEstadia.desde(reserva);
        List<LocalDate> dias = new ArrayList<>();
        for (LocalDate d = reserva.getFechaInicio(); !d.isAfter(reserva.getFechaFin()); d = d.plusDays(1)) {
            dias.add(d);
        }
        ValidadorPlanService.Resultado resultado = new ValidadorPlanService().validar(planCompleto(), contexto(dias),
                estadia, dias, preferencias(), "modelo-falso");
        try {
            return PlanVisita.builder()
                    .id(5L)
                    .reserva(reserva)
                    .preferencias(objectMapper.writeValueAsString(preferencias()))
                    .contenido(objectMapper.writeValueAsString(new ContenidoPlan("Original summary",
                            List.of(new EntendidoItem("Saved understanding.", "From the first generation.")),
                            resultado.dias(), enRevision)))
                    .descripcion("Our anniversary trip.")
                    .fuentesUsadas("OPEN_METEO")
                    .fuentesNoDisponibles("TICKETMASTER")
                    .requiereRevision(!enRevision.isEmpty())
                    .fechaGeneracion(LocalDateTime.of(2026, 11, 10, 9, 0))
                    .fechaActualizacion(LocalDateTime.of(2026, 11, 10, 9, 0))
                    .generacionesHoy(generacionesHoy)
                    .fechaUltimaGeneracion(ultima)
                    .build();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private ContenidoPlan leer(PlanVisita plan) throws Exception {
        return objectMapper.readValue(plan.getContenido(), ContenidoPlan.class);
    }

    private static BloquePlanResponse bloque(PlanVisitaResponse respuesta, TipoBloque tipo) {
        return respuesta.dias().stream().flatMap(d -> d.bloques().stream())
                .filter(b -> b.tipo() == tipo).findFirst().orElseThrow();
    }

    private static ContextoPlan contexto(List<LocalDate> dias) {
        Map<LocalDate, ClimaDia> clima = new LinkedHashMap<>();
        dias.forEach(d -> clima.put(d, new ClimaDia(d, "Cold, usually dry", 6, 0, 10, TipoClima.CLIMA_TIPICO)));
        return new ContextoPlan(dias, clima,
                List.of(new LugarContexto(1L, "The Museum of Modern Art (MoMA)", CategoriaLugar.MUSEO, true, 150,
                                "Daily", "https://www.moma.org", 270),
                        new LugarContexto(2L, "Central Park", CategoriaLugar.PARQUE, false, 120,
                                "Daily", "https://www.centralparknyc.org", 2700)),
                List.of(),
                List.of(new ServicioContexto(1L, "Wellness", 45.0, List.of(MASAJE))),
                Arrays.stream(RestauranteHotel.values()).map(r -> new RestauranteContexto(r, HorariosRestaurantes.nombre(r),
                        HorariosRestaurantes.horario(r), List.of())).toList(),
                List.of(FuentePlan.OPEN_METEO, FuentePlan.NYC_OPEN_DATA), List.of(FuentePlan.TICKETMASTER));
    }

    private static Reserva reserva(Long id, Cliente cliente, EstadoReserva estado, LocalDate inicio, LocalDate fin) {
        TipoHabitacion luxury = TipoHabitacion.builder().id(4L).nombre("Luxury").build();
        Habitacion habitacion = Habitacion.builder().id(31L).numero("703").tipoHabitacion(luxury).build();
        return Reserva.builder().id(id).cliente(cliente).habitacion(habitacion).fechaInicio(inicio).fechaFin(fin)
                .estado(estado).cantidadPersonas(2).build();
    }

    // Gemini falso: responde los planes programados y guarda el texto de cada solicitud
    private class GeminiFalso implements GeminiClient {

        private final Deque<String> respuestas = new ArrayDeque<>();
        private final List<String> solicitudes = new ArrayList<>();

        void responder(PlanGenerado plan) throws Exception {
            respuestas.add(objectMapper.writeValueAsString(plan));
        }

        String ultimaSolicitud() {
            return solicitudes.get(solicitudes.size() - 1);
        }

        @Override
        public void transmitir(SolicitudGemini solicitud, java.util.function.Consumer<ParteGemini> alRecibirParte) {
            throw new UnsupportedOperationException();
        }

        @Override
        public RespuestaJson generarJson(SolicitudGemini solicitud, Map<String, Object> esquema) {
            solicitudes.add(solicitud.instruccionSistema() + "\n" + solicitud.contenidos().get(0).partes().get(0).texto());
            return new RespuestaJson(respuestas.isEmpty() ? "{\"resumen\":\"\",\"dias\":[]}" : respuestas.poll(), "modelo-falso");
        }
    }
}
