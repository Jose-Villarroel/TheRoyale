package com.theroyale.backend.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theroyale.backend.dto.CrearResenaRequest;
import com.theroyale.backend.dto.ResumenResenasResponse;
import com.theroyale.backend.errors.AccesoDenegadoException;
import com.theroyale.backend.errors.RecursoNoEncontradoException;
import com.theroyale.backend.model.Cliente;
import com.theroyale.backend.model.EstadoHabitacion;
import com.theroyale.backend.model.EstadoReserva;
import com.theroyale.backend.model.Habitacion;
import com.theroyale.backend.model.Resena;
import com.theroyale.backend.model.Reserva;
import com.theroyale.backend.model.TipoHabitacion;
import com.theroyale.backend.service.ResenaService;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ===== Pruebas de ResenaRestController con mocks =====
// @WebMvcTest levanta solo el controlador y ApiExceptionHandler; ResenaService se simula.
// Se cubren los códigos 201/200/204/400/403/404/409, la precedencia de los parámetros del GET
// y la forma del JSON: aplanado y sin datos privados del autor (email, cédula, teléfono).
@WebMvcTest(
        controllers = ResenaRestController.class,
        excludeAutoConfiguration = ThymeleafAutoConfiguration.class
)
@ActiveProfiles("test")
class ResenaRestControllerTest {

    private static final String COMENTARIO = "Spotless room and very attentive staff.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ResenaService resenaService;

    // ===== POST: crear =====
    @Test
    void resenaController_crear_retorna201ConJsonPlanoSinDatosPrivados() throws Exception {
        // Arrange
        CrearResenaRequest solicitud = new CrearResenaRequest(12L, 8L, 5, "Perfect city view", COMENTARIO);
        when(resenaService.crear(any(CrearResenaRequest.class))).thenReturn(crearResena(3L, true));

        // Act & Assert
        mockMvc.perform(post("/api/resenas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitud)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(3)))
                .andExpect(jsonPath("$.calificacion", is(5)))
                .andExpect(jsonPath("$.titulo", is("Perfect city view")))
                .andExpect(jsonPath("$.comentario", is(COMENTARIO)))
                .andExpect(jsonPath("$.visible", is(true)))
                .andExpect(jsonPath("$.fechaCreacion", is("2026-11-10T14:02:11")))
                .andExpect(jsonPath("$.fechaModificacion", nullValue()))
                .andExpect(jsonPath("$.reservaId", is(12)))
                .andExpect(jsonPath("$.fechaInicioEstadia", is("2026-11-03")))
                .andExpect(jsonPath("$.fechaFinEstadia", is("2026-11-06")))
                .andExpect(jsonPath("$.clienteId", is(8)))
                .andExpect(jsonPath("$.clienteNombre", is("Laura M.")))
                .andExpect(jsonPath("$.tipoHabitacionId", is(4)))
                .andExpect(jsonPath("$.tipoHabitacionNombre", is("Luxury")))
                // Privacidad: nada que identifique al autor más allá de "Laura M."
                .andExpect(jsonPath("$.clienteEmail").doesNotExist())
                .andExpect(jsonPath("$.clienteCedula").doesNotExist())
                .andExpect(jsonPath("$.clienteTelefono").doesNotExist())
                .andExpect(jsonPath("$.clienteApellido").doesNotExist())
                .andExpect(jsonPath("$.cliente").doesNotExist())
                .andExpect(jsonPath("$.reserva").doesNotExist());
    }

    @Test
    void resenaController_crear_datosInvalidos_retorna400() throws Exception {
        // Arrange
        when(resenaService.crear(any(CrearResenaRequest.class)))
                .thenThrow(new IllegalArgumentException("The rating must be a whole number between 1 and 5."));

        // Act & Assert
        mockMvc.perform(post("/api/resenas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CrearResenaRequest(12L, 8L, 6, null, COMENTARIO))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    void resenaController_crear_reservaAjena_retorna403ConMensaje() throws Exception {
        // Arrange
        when(resenaService.crear(any(CrearResenaRequest.class)))
                .thenThrow(new AccesoDenegadoException("This reservation does not belong to you."));

        // Act & Assert
        mockMvc.perform(post("/api/resenas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CrearResenaRequest(12L, 9L, 5, null, COMENTARIO))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.message", is("This reservation does not belong to you.")));
    }

    @Test
    void resenaController_crear_reservaNoFinalizada_retorna409ConMensaje() throws Exception {
        // Arrange
        when(resenaService.crear(any(CrearResenaRequest.class)))
                .thenThrow(new IllegalStateException("Only completed stays can be reviewed."));

        // Act & Assert
        mockMvc.perform(post("/api/resenas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CrearResenaRequest(1L, 8L, 5, null, COMENTARIO))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Only completed stays can be reviewed.")));
    }

    // ===== GET: listados y precedencia de parámetros =====
    @Test
    void resenaController_listar_porTipo_retorna200ConLasVisibles() throws Exception {
        // Arrange
        when(resenaService.listarPorTipo(4L)).thenReturn(List.of(crearResena(3L, true)));

        // Act & Assert
        mockMvc.perform(get("/api/resenas").param("tipoHabitacionId", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(3)))
                .andExpect(jsonPath("$[0].clienteNombre", is("Laura M.")))
                .andExpect(jsonPath("$[0].clienteEmail").doesNotExist());
    }

    @Test
    void resenaController_listar_porCliente_tienePrecedenciaSobreElTipo() throws Exception {
        // Arrange
        when(resenaService.listarPorCliente(8L)).thenReturn(List.of(crearResena(3L, false)));

        // Act & Assert
        mockMvc.perform(get("/api/resenas").param("clienteId", "8").param("tipoHabitacionId", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].visible", is(false)));
        verify(resenaService).listarPorCliente(8L);
    }

    @Test
    void resenaController_listar_incluirOcultas_usaModeracionConFiltros() throws Exception {
        // Arrange
        when(resenaService.listarParaModeracion(4L, 1)).thenReturn(List.of(crearResena(3L, false)));

        // Act & Assert
        mockMvc.perform(get("/api/resenas")
                        .param("incluirOcultas", "true")
                        .param("tipoHabitacionId", "4")
                        .param("calificacion", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        verify(resenaService).listarParaModeracion(4L, 1);
    }

    @Test
    void resenaController_listar_sinParametros_retorna400() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/resenas"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(resenaService);
    }

    @Test
    void resenaController_obtenerPorId_inexistente_retorna404() throws Exception {
        // Arrange
        when(resenaService.obtenerPorId(99L)).thenThrow(new RecursoNoEncontradoException("Review not found: 99"));

        // Act & Assert
        mockMvc.perform(get("/api/resenas/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Review not found: 99")));
    }

    // ===== GET: resúmenes =====
    @Test
    void resenaController_resumen_deUnTipo_retorna200ConDistribucionCompleta() throws Exception {
        // Arrange
        Map<Integer, Long> distribucion = new LinkedHashMap<>(Map.of(1, 0L, 2, 0L, 3, 0L, 4, 1L, 5, 2L));
        when(resenaService.resumen(4L)).thenReturn(new ResumenResenasResponse(4L, 4.7, 3, distribucion));

        // Act & Assert
        mockMvc.perform(get("/api/resenas/resumen").param("tipoHabitacionId", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoHabitacionId", is(4)))
                .andExpect(jsonPath("$.promedio", is(4.7)))
                .andExpect(jsonPath("$.total", is(3)))
                .andExpect(jsonPath("$.distribucion.1", is(0)))
                .andExpect(jsonPath("$.distribucion.5", is(2)));
    }

    @Test
    void resenaController_resumenTodos_retorna200ConUnaFilaPorTipo() throws Exception {
        // Arrange
        Map<Integer, Long> vacia = Map.of(1, 0L, 2, 0L, 3, 0L, 4, 0L, 5, 0L);
        when(resenaService.resumenTodos()).thenReturn(List.of(new ResumenResenasResponse(1L, null, 0, vacia)));

        // Act & Assert
        mockMvc.perform(get("/api/resenas/resumen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].promedio", nullValue()))
                .andExpect(jsonPath("$[0].total", is(0)));
    }

    // ===== PUT: editar =====
    @Test
    void resenaController_actualizar_porElAutor_retorna200() throws Exception {
        // Arrange
        Resena editada = crearResena(3L, true);
        editada.setCalificacion(3);
        editada.setFechaModificacion(LocalDateTime.of(2026, 11, 11, 9, 0));
        when(resenaService.actualizar(eq(3L), any(CrearResenaRequest.class))).thenReturn(editada);

        // Act & Assert
        mockMvc.perform(put("/api/resenas/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CrearResenaRequest(null, 8L, 3, null, COMENTARIO))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calificacion", is(3)))
                .andExpect(jsonPath("$.fechaModificacion", is("2026-11-11T09:00:00")));
    }

    @Test
    void resenaController_actualizar_porOtroCliente_retorna403() throws Exception {
        // Arrange
        when(resenaService.actualizar(eq(3L), any(CrearResenaRequest.class)))
                .thenThrow(new AccesoDenegadoException("Only the author can edit or delete this review."));

        // Act & Assert
        mockMvc.perform(put("/api/resenas/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CrearResenaRequest(null, 9L, 3, null, COMENTARIO))))
                .andExpect(status().isForbidden());
    }

    // ===== DELETE: eliminar =====
    @Test
    void resenaController_eliminar_porElAutor_retorna204() throws Exception {
        // Arrange
        doNothing().when(resenaService).eliminar(3L, 8L);

        // Act & Assert
        mockMvc.perform(delete("/api/resenas/3").param("clienteId", "8"))
                .andExpect(status().isNoContent());
        verify(resenaService).eliminar(3L, 8L);
    }

    @Test
    void resenaController_eliminar_porOtroCliente_retorna403() throws Exception {
        // Arrange
        doThrow(new AccesoDenegadoException("Only the author can edit or delete this review."))
                .when(resenaService).eliminar(3L, 9L);

        // Act & Assert
        mockMvc.perform(delete("/api/resenas/3").param("clienteId", "9"))
                .andExpect(status().isForbidden());
    }

    @Test
    void resenaController_eliminar_sinClienteId_retorna400() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/resenas/3"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(resenaService);
    }

    // ===== Moderación =====
    @Test
    void resenaController_ocultar_retorna200ConVisibleFalse() throws Exception {
        // Arrange
        when(resenaService.ocultar(3L)).thenReturn(crearResena(3L, false));

        // Act & Assert
        mockMvc.perform(post("/api/resenas/3/ocultar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visible", is(false)));
    }

    @Test
    void resenaController_mostrar_inexistente_retorna404() throws Exception {
        // Arrange
        when(resenaService.mostrar(99L)).thenThrow(new RecursoNoEncontradoException("Review not found: 99"));

        // Act & Assert
        mockMvc.perform(post("/api/resenas/99/mostrar"))
                .andExpect(status().isNotFound());
    }

    // Reseña completa del ejemplo de la especificación; el cliente trae datos privados a propósito
    // para comprobar que no salen en el JSON
    private Resena crearResena(Long id, boolean visible) {
        TipoHabitacion luxury = TipoHabitacion.builder().id(4L).nombre("Luxury").build();
        Habitacion habitacion = Habitacion.builder()
                .id(70L).numero("704").tipoHabitacion(luxury)
                .precio(370.0).estado(EstadoHabitacion.DISPONIBLE)
                .build();
        Cliente laura = Cliente.builder()
                .id(8L).nombre("Laura").apellido("Martínez")
                .email("laura@email.com").cedula("1001").telefono("+1-555-0101")
                .build();
        Reserva reserva = Reserva.builder()
                .id(12L).cliente(laura).habitacion(habitacion)
                .fechaInicio(LocalDate.of(2026, 11, 3)).fechaFin(LocalDate.of(2026, 11, 6))
                .estado(EstadoReserva.FINALIZADA).cantidadPersonas(2)
                .precioNocheAcordado(BigDecimal.valueOf(370))
                .fechaCreacion(LocalDateTime.of(2026, 10, 1, 10, 0))
                .build();
        return Resena.builder()
                .id(id).reserva(reserva).cliente(laura).tipoHabitacion(luxury)
                .calificacion(5).titulo("Perfect city view").comentario(COMENTARIO)
                .visible(visible)
                .fechaCreacion(LocalDateTime.of(2026, 11, 10, 14, 2, 11))
                .build();
    }
}
