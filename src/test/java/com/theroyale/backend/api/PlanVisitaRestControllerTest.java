package com.theroyale.backend.api;

import com.theroyale.backend.dto.CuentaResponse;
import com.theroyale.backend.dto.plan.AgregarBloqueRequest;
import com.theroyale.backend.dto.plan.AjustarPlanRequest;
import com.theroyale.backend.dto.plan.BloquePlanResponse;
import com.theroyale.backend.dto.plan.ClimaDiaResponse;
import com.theroyale.backend.dto.plan.DiaPlanResponse;
import com.theroyale.backend.dto.plan.EntendidoItem;
import com.theroyale.backend.dto.plan.GenerarPlanRequest;
import com.theroyale.backend.dto.plan.InterpretacionPlan;
import com.theroyale.backend.dto.plan.InterpretarPlanRequest;
import com.theroyale.backend.dto.plan.PlanVisitaResponse;
import com.theroyale.backend.dto.plan.PreferenciasPlan;
import com.theroyale.backend.dto.plan.RegenerarDiaRequest;
import com.theroyale.backend.errors.AccesoDenegadoException;
import com.theroyale.backend.errors.LimiteExcedidoException;
import com.theroyale.backend.errors.PlanNoConfiableException;
import com.theroyale.backend.errors.RecursoNoEncontradoException;
import com.theroyale.backend.errors.ServicioNoDisponibleException;
import com.theroyale.backend.model.EstadoCuenta;
import com.theroyale.backend.model.EstadoReserva;
import com.theroyale.backend.model.EtiquetaPlato;
import com.theroyale.backend.model.FuentePlan;
import com.theroyale.backend.model.Interes;
import com.theroyale.backend.model.Presupuesto;
import com.theroyale.backend.model.Ritmo;
import com.theroyale.backend.model.TipoBloque;
import com.theroyale.backend.model.TipoClima;
import com.theroyale.backend.service.plan.PlanVisitaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ===== Pruebas de PlanVisitaRestController con mocks =====
// @WebMvcTest levanta solo el controlador y ApiExceptionHandler; PlanVisitaService se simula.
// Se cubren los códigos 201/200/204/400/403/404/409/429/502/503 y la forma del JSON del contrato.
@WebMvcTest(
        controllers = PlanVisitaRestController.class,
        excludeAutoConfiguration = ThymeleafAutoConfiguration.class
)
@ActiveProfiles("test")
class PlanVisitaRestControllerTest {

    private static final String PLAN = "/api/reservas/12/plan";
    private static final String CUERPO_GENERAR = """
            {"clienteId":8,"preferencias":{"intereses":["MUSEOS","TEATRO"],"ritmo":"EQUILIBRADO",
             "presupuesto":"ALTO","dieta":["VEGETARIANO"],"movilidadReducida":false,"conNinos":false,"notas":null},
             "descripcion":"Our anniversary","entendido":[{"titulo":"An anniversary.","detalle":"Dinner."}]}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlanVisitaService planVisitaService;

    // ===== Estado =====
    @Test
    void planVisitaController_estado_retornaDisponible() throws Exception {
        // Arrange
        when(planVisitaService.estaDisponible()).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/api/plan/estado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible", is(false)));
    }

    // ===== Generar =====
    @Test
    void planVisitaController_generar_retorna201ConElPlanDelContrato() throws Exception {
        // Arrange
        when(planVisitaService.generar(eq(12L), any(GenerarPlanRequest.class))).thenReturn(plan());

        // Act & Assert
        mockMvc.perform(post(PLAN).contentType(MediaType.APPLICATION_JSON).content(CUERPO_GENERAR))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservaId", is(12)))
                .andExpect(jsonPath("$.estadoReserva", is("CONFIRMADA")))
                .andExpect(jsonPath("$.fechaInicio", is("2026-12-12")))
                .andExpect(jsonPath("$.noches", is(3)))
                .andExpect(jsonPath("$.tipoHabitacionNombre", is("Luxury")))
                .andExpect(jsonPath("$.cantidadPersonas", is(2)))
                .andExpect(jsonPath("$.entendido[0].titulo", is("An anniversary.")))
                .andExpect(jsonPath("$.preferencias.intereses[1]", is("TEATRO")))
                .andExpect(jsonPath("$.requiereRevision", is(false)))
                .andExpect(jsonPath("$.diasEnRevision", hasSize(0)))
                .andExpect(jsonPath("$.fuentesUsadas[0]", is("OPEN_METEO")))
                .andExpect(jsonPath("$.fuentesNoDisponibles[0]", is("TICKETMASTER")))
                .andExpect(jsonPath("$.generacionesRestantesHoy", is(4)))
                .andExpect(jsonPath("$.fechaGeneracion", is("2026-11-11T14:02:11")))
                .andExpect(jsonPath("$.dias[0].fecha", is("2026-12-12")))
                .andExpect(jsonPath("$.dias[0].tipoDia", is("LLEGADA")))
                .andExpect(jsonPath("$.dias[0].clima.tipo", is("CLIMA_TIPICO")))
                .andExpect(jsonPath("$.dias[0].bloques[0].tipo", is("TIEMPO_LIBRE")))
                .andExpect(jsonPath("$.dias[0].bloques[0].traslado", nullValue()))
                .andExpect(jsonPath("$.dias[0].bloques[1].inicio", is("16:00")))
                .andExpect(jsonPath("$.dias[0].bloques[1].servicioId", is(1)))
                .andExpect(jsonPath("$.dias[0].bloques[1].precio", is(45.0)))
                .andExpect(jsonPath("$.dias[0].bloques[1].agregable", is(true)))
                .andExpect(jsonPath("$.dias[0].bloques[1].traslado", is("In the hotel")))
                // Sin datos personales del huésped
                .andExpect(jsonPath("$.clienteEmail").doesNotExist())
                .andExpect(jsonPath("$.clienteNombre").doesNotExist());
    }

    @Test
    void planVisitaController_generar_errores_retornanSuCodigo() throws Exception {
        // Arrange: cada llamada falla con un error distinto
        when(planVisitaService.generar(eq(12L), any(GenerarPlanRequest.class)))
                .thenThrow(new IllegalArgumentException("Choose between 1 and 5 interests."))
                .thenThrow(new AccesoDenegadoException("This reservation does not belong to you."))
                .thenThrow(new RecursoNoEncontradoException("Reserva no encontrada: 12"))
                .thenThrow(new IllegalStateException("Only pending, confirmed or in-progress reservations can have a visit plan."))
                .thenThrow(new LimiteExcedidoException("You have reached today's limit of 5 plans for this stay. Try again tomorrow."))
                .thenThrow(new PlanNoConfiableException())
                .thenThrow(new ServicioNoDisponibleException("The planner is unavailable right now."));

        // Act & Assert
        generarYEsperar(400, "Choose between 1 and 5 interests.");
        generarYEsperar(403, "This reservation does not belong to you.");
        generarYEsperar(404, "Reserva no encontrada: 12");
        generarYEsperar(409, "Only pending, confirmed or in-progress reservations can have a visit plan.");
        generarYEsperar(429, "You have reached today's limit of 5 plans for this stay. Try again tomorrow.");
        generarYEsperar(502, "We could not build a reliable plan. Please try again.");
        generarYEsperar(503, "The planner is unavailable right now.");
    }

    @Test
    void planVisitaController_generar_enumInvalido_retorna400SinLlamarAlServicio() throws Exception {
        // Act & Assert
        mockMvc.perform(post(PLAN).contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_GENERAR.replace("EQUILIBRADO", "RAPIDO")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    // ===== Interpretar =====
    @Test
    void planVisitaController_interpretar_retorna200YPasaLaIp() throws Exception {
        // Arrange
        when(planVisitaService.interpretar(eq(12L), any(InterpretarPlanRequest.class), anyString())).thenReturn(
                new InterpretacionPlan(preferencias(), List.of(new EntendidoItem("Un aniversario.", "Cena el sábado."))));

        // Act & Assert
        mockMvc.perform(post(PLAN + "/interpretar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":8,\"descripcion\":\"Es nuestro aniversario\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferencias.ritmo", is("EQUILIBRADO")))
                .andExpect(jsonPath("$.preferencias.dieta[0]", is("VEGETARIANO")))
                .andExpect(jsonPath("$.entendido[0].detalle", is("Cena el sábado.")));
        verify(planVisitaService).interpretar(eq(12L), any(InterpretarPlanRequest.class), eq("127.0.0.1"));
    }

    @Test
    void planVisitaController_interpretar_limitePorIp_retorna429() throws Exception {
        // Arrange
        when(planVisitaService.interpretar(eq(12L), any(InterpretarPlanRequest.class), anyString()))
                .thenThrow(new LimiteExcedidoException("Too many messages, please try again in a few minutes."));

        // Act & Assert
        mockMvc.perform(post(PLAN + "/interpretar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":8,\"descripcion\":\"Es nuestro aniversario\"}"))
                .andExpect(status().isTooManyRequests());
    }

    // ===== Obtener, ajustar, regenerar, eliminar =====
    @Test
    void planVisitaController_obtener_retorna200O404() throws Exception {
        // Arrange
        when(planVisitaService.obtener(12L, 8L)).thenReturn(plan());
        when(planVisitaService.obtener(12L, 9L)).thenThrow(new RecursoNoEncontradoException("No plan yet for this reservation."));

        // Act & Assert
        mockMvc.perform(get(PLAN).param("clienteId", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dias", hasSize(1)));
        mockMvc.perform(get(PLAN).param("clienteId", "9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("No plan yet for this reservation.")));
        mockMvc.perform(get(PLAN))
                .andExpect(status().isBadRequest());
    }

    @Test
    void planVisitaController_ajustar_retorna200() throws Exception {
        // Arrange
        when(planVisitaService.ajustar(eq(12L), any(AjustarPlanRequest.class))).thenReturn(plan());

        // Act & Assert
        mockMvc.perform(post(PLAN + "/ajustar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":8,\"indicacion\":\"More museums\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservaId", is(12)));
    }

    @Test
    void planVisitaController_regenerarDia_retorna200YFechaInvalidaDa400() throws Exception {
        // Arrange
        when(planVisitaService.regenerarDia(eq(12L), eq(LocalDate.of(2026, 12, 13)), any(RegenerarDiaRequest.class)))
                .thenReturn(plan());

        // Act & Assert
        mockMvc.perform(post(PLAN + "/dias/2026-12-13/regenerar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":8,\"indicacion\":\"something quieter\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post(PLAN + "/dias/13-12-2026/regenerar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":8}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void planVisitaController_eliminar_retorna204O403() throws Exception {
        // Arrange
        doThrow(new AccesoDenegadoException("This reservation does not belong to you."))
                .when(planVisitaService).eliminar(12L, 9L);

        // Act & Assert
        mockMvc.perform(delete(PLAN).param("clienteId", "8")).andExpect(status().isNoContent());
        mockMvc.perform(delete(PLAN).param("clienteId", "9")).andExpect(status().isForbidden());
        verify(planVisitaService).eliminar(12L, 8L);
    }

    // ===== Add to my stay =====
    @Test
    void planVisitaController_agregarACuenta_retorna201ConLaCuenta() throws Exception {
        // Arrange
        CuentaResponse cuenta = new CuentaResponse(3L, 12L, EstadoCuenta.ABIERTA, LocalDateTime.of(2026, 11, 10, 15, 0),
                List.of(), List.of(), BigDecimal.valueOf(45), BigDecimal.ZERO, BigDecimal.valueOf(45));
        when(planVisitaService.agregarACuenta(eq(12L), eq("b-1"), any(AgregarBloqueRequest.class))).thenReturn(cuenta);

        // Act & Assert
        mockMvc.perform(post(PLAN + "/bloques/b-1/agregar-a-cuenta").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":8}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservaId", is(12)))
                .andExpect(jsonPath("$.saldo", is(45)));
    }

    @Test
    void planVisitaController_agregarACuenta_cuentaNoAbiertaONoEsServicio() throws Exception {
        // Arrange
        when(planVisitaService.agregarACuenta(eq(12L), eq("pendiente"), any(AgregarBloqueRequest.class)))
                .thenThrow(new IllegalStateException("This reservation does not have an account yet: confirm it first."));
        when(planVisitaService.agregarACuenta(eq(12L), eq("lugar"), any(AgregarBloqueRequest.class)))
                .thenThrow(new IllegalArgumentException("Only hotel services can be added to your stay."));

        // Act & Assert
        mockMvc.perform(post(PLAN + "/bloques/pendiente/agregar-a-cuenta").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":8}"))
                .andExpect(status().isConflict());
        mockMvc.perform(post(PLAN + "/bloques/lugar/agregar-a-cuenta").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":8}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Only hotel services can be added to your stay.")));
    }

    // ===== Ayudas =====
    private void generarYEsperar(int codigo, String mensaje) throws Exception {
        mockMvc.perform(post(PLAN).contentType(MediaType.APPLICATION_JSON).content(CUERPO_GENERAR))
                .andExpect(status().is(codigo))
                .andExpect(jsonPath("$.status", is(codigo)))
                .andExpect(jsonPath("$.message", is(mensaje)));
    }

    private static PreferenciasPlan preferencias() {
        return new PreferenciasPlan(List.of(Interes.MUSEOS, Interes.TEATRO), Ritmo.EQUILIBRADO, Presupuesto.ALTO,
                List.of(EtiquetaPlato.VEGETARIANO), false, false, null);
    }

    private static PlanVisitaResponse plan() {
        BloquePlanResponse checkIn = new BloquePlanResponse("n-1", "15:00", "15:30", TipoBloque.TIEMPO_LIBRE, "FREE TIME", "",
                "Check-in from 15:00. Your suite will be ready when you arrive.", null, null, null, null, null, null, null, null);
        BloquePlanResponse masaje = new BloquePlanResponse("b-1", "16:00", "17:30", TipoBloque.SERVICIO_HOTEL, "SPA",
                "Signature Massage", "Rest after the flight.", "In the hotel", true, null, null, "The Royale", 1L, 45.0, true);
        DiaPlanResponse dia = new DiaPlanResponse(LocalDate.of(2026, 12, 12), 1, "LLEGADA", "An easy first evening",
                new ClimaDiaResponse("Cold, usually dry", 5, -1, 10, TipoClima.CLIMA_TIPICO), List.of(checkIn, masaje));
        return new PlanVisitaResponse(12L, EstadoReserva.CONFIRMADA, LocalDate.of(2026, 12, 12), LocalDate.of(2026, 12, 15),
                3, "Luxury", 2, "Happy anniversary.", "Our anniversary",
                List.of(new EntendidoItem("An anniversary.", "Dinner on Saturday.")), preferencias(), false, List.of(),
                List.of(FuentePlan.OPEN_METEO, FuentePlan.NYC_OPEN_DATA), List.of(FuentePlan.TICKETMASTER), 4,
                LocalDateTime.of(2026, 11, 11, 14, 2, 11), LocalDateTime.of(2026, 11, 11, 14, 2, 11), List.of(dia));
    }
}
