package com.theroyale.backend.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theroyale.backend.dto.CrearReservaRequest;
import com.theroyale.backend.errors.RecursoNoEncontradoException;
import com.theroyale.backend.model.Cliente;
import com.theroyale.backend.model.EstadoHabitacion;
import com.theroyale.backend.model.EstadoReserva;
import com.theroyale.backend.model.Habitacion;
import com.theroyale.backend.model.Reserva;
import com.theroyale.backend.model.TipoHabitacion;
import com.theroyale.backend.service.ReservaService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ===== Pruebas de ReservaRestController con mocks =====
// @WebMvcTest levanta solo la capa web (el controlador y ApiExceptionHandler) y el servicio se simula
// con @MockBean. Se cubren los 5 tipos de consulta: POST, GET de lista, GET por id, PUT y DELETE.
// Se excluye la autoconfiguración de Thymeleaf para evitar que intente resolver vistas HTML.
@WebMvcTest(
        controllers = ReservaRestController.class,
        excludeAutoConfiguration = ThymeleafAutoConfiguration.class
)
@ActiveProfiles("test")
class ReservaRestControllerTest {

    private static final LocalDate INICIO = LocalDate.of(2030, 6, 10);
    private static final LocalDate FIN = LocalDate.of(2030, 6, 13);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReservaService reservaService;

    // ===== 1. POST: crear =====
    @Test
    void reservaController_crear_retorna201ConJsonPlano() throws Exception {
        // Arrange
        CrearReservaRequest solicitud = new CrearReservaRequest(1L, 1L, INICIO, FIN, 2);
        when(reservaService.crear(any(CrearReservaRequest.class))).thenReturn(crearReserva(1L, 2));

        // Act & Assert: cliente y habitación van aplanados, sin objetos anidados
        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitud)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.estado", is("PENDIENTE")))
                .andExpect(jsonPath("$.clienteNombre", is("Ana")))
                .andExpect(jsonPath("$.habitacionNumero", is("101")))
                .andExpect(jsonPath("$.cliente").doesNotExist())
                .andExpect(jsonPath("$.habitacion").doesNotExist());
    }

    // ===== 2. GET: todas =====
    @Test
    void reservaController_listar_retorna200YLaLista() throws Exception {
        // Arrange
        when(reservaService.listar(null)).thenReturn(List.of(crearReserva(1L, 2), crearReserva(2L, 4)));

        // Act & Assert
        mockMvc.perform(get("/api/reservas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[1].id", is(2)));
    }

    // ===== 3. GET: una por id, cuando existe =====
    @Test
    void reservaController_obtenerPorId_existente_retorna200() throws Exception {
        // Arrange
        when(reservaService.obtenerPorId(1L)).thenReturn(crearReserva(1L, 2));

        // Act & Assert
        mockMvc.perform(get("/api/reservas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.tipoHabitacionNombre", is("Suite")));
    }

    // ===== 3. GET: una por id, cuando no existe =====
    @Test
    void reservaController_obtenerPorId_inexistente_retorna404ConMensaje() throws Exception {
        // Arrange
        when(reservaService.obtenerPorId(99L)).thenThrow(new RecursoNoEncontradoException("Reserva no encontrada: 99"));

        // Act & Assert: ApiExceptionHandler convierte la excepción en el JSON que lee Angular
        mockMvc.perform(get("/api/reservas/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Reserva no encontrada: 99")));
    }

    // ===== 4. PUT: modificar =====
    @Test
    void reservaController_modificar_retorna200ConLosDatosNuevos() throws Exception {
        // Arrange
        CrearReservaRequest cambios = new CrearReservaRequest(1L, 1L, INICIO, FIN, 4);
        when(reservaService.modificar(eq(1L), any(CrearReservaRequest.class))).thenReturn(crearReserva(1L, 4));

        // Act & Assert
        mockMvc.perform(put("/api/reservas/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cambios)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.cantidadPersonas", is(4)));
    }

    // ===== 5. DELETE: eliminar =====
    @Test
    void reservaController_eliminar_retorna204() throws Exception {
        // Arrange
        doNothing().when(reservaService).eliminar(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/reservas/1"))
                .andExpect(status().isNoContent());
        verify(reservaService).eliminar(1L);
    }

    // Reserva completa porque ReservaResponse lee los datos de su cliente, habitación y tipo
    private Reserva crearReserva(Long id, int personas) {
        TipoHabitacion suite = TipoHabitacion.builder().id(1L).nombre("Suite").build();
        Habitacion habitacion = Habitacion.builder()
                .id(1L)
                .numero("101")
                .tipoHabitacion(suite)
                .precio(200.0)
                .estado(EstadoHabitacion.DISPONIBLE)
                .build();
        Cliente cliente = Cliente.builder()
                .id(1L)
                .nombre("Ana")
                .apellido("Prueba")
                .email("ana@email.com")
                .cedula("1001")
                .build();
        return Reserva.builder()
                .id(id)
                .cliente(cliente)
                .habitacion(habitacion)
                .fechaInicio(INICIO)
                .fechaFin(FIN)
                .estado(EstadoReserva.PENDIENTE)
                .cantidadPersonas(personas)
                .precioNocheAcordado(BigDecimal.valueOf(200))
                .fechaCreacion(LocalDateTime.of(2030, 1, 15, 10, 0))
                .build();
    }
}
