package com.theroyale.backend.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theroyale.backend.dto.PlatoMenuRequest;
import com.theroyale.backend.errors.RecursoNoEncontradoException;
import com.theroyale.backend.model.CategoriaPlato;
import com.theroyale.backend.model.EtiquetaPlato;
import com.theroyale.backend.model.PlatoMenu;
import com.theroyale.backend.model.RestauranteHotel;
import com.theroyale.backend.service.PlatoMenuService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

// ===== Pruebas de PlatoMenuRestController con mocks =====
// @WebMvcTest levanta solo el controlador y ApiExceptionHandler; PlatoMenuService se simula.
// Cubre los 5 tipos de consulta (GET lista, GET por id, POST, PUT, DELETE), los filtros del GET,
// la forma del JSON (enums por nombre) y los errores 400/404.
@WebMvcTest(PlatoMenuRestController.class)
@ActiveProfiles("test")
class PlatoMenuRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PlatoMenuService platoMenuService;

    // ===== GET lista =====
    @Test
    void platoMenuController_listar_conFiltros_retorna200ConEnumsPorNombre() throws Exception {
        // Arrange
        when(platoMenuService.listar(RestauranteHotel.ROOM_SERVICE, null, EtiquetaPlato.VEGANO, true))
                .thenReturn(List.of(crearPlato(3L, "Buddha Bowl")));

        // Act & Assert
        mockMvc.perform(get("/api/menu")
                        .param("restaurante", "ROOM_SERVICE")
                        .param("etiqueta", "VEGANO")
                        .param("soloDisponibles", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(3)))
                .andExpect(jsonPath("$[0].nombre", is("Buddha Bowl")))
                .andExpect(jsonPath("$[0].precio", is(24.5)))
                .andExpect(jsonPath("$[0].restaurante", is("ROOM_SERVICE")))
                .andExpect(jsonPath("$[0].categoria", is("PRINCIPAL")))
                .andExpect(jsonPath("$[0].etiquetas", contains("VEGANO", "SIN_GLUTEN")))
                .andExpect(jsonPath("$[0].disponible", is(true)))
                .andExpect(jsonPath("$[0].imagenUrl", nullValue()));
    }

    @Test
    void platoMenuController_listar_sinParametros_usaSoloDisponiblesFalse() throws Exception {
        // Arrange
        when(platoMenuService.listar(null, null, null, false)).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/menu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        verify(platoMenuService).listar(null, null, null, false);
    }

    @Test
    void platoMenuController_listar_restauranteDesconocido_retorna400() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/menu").param("restaurante", "FOOD_TRUCK"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
        verifyNoInteractions(platoMenuService);
    }

    // ===== GET por id =====
    @Test
    void platoMenuController_obtenerPorId_existente_retorna200() throws Exception {
        // Arrange
        when(platoMenuService.obtenerPorId(3L)).thenReturn(crearPlato(3L, "Buddha Bowl"));

        // Act & Assert
        mockMvc.perform(get("/api/menu/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre", is("Buddha Bowl")));
    }

    @Test
    void platoMenuController_obtenerPorId_inexistente_retorna404() throws Exception {
        // Arrange
        when(platoMenuService.obtenerPorId(99L)).thenThrow(new RecursoNoEncontradoException("Dish not found: 99"));

        // Act & Assert
        mockMvc.perform(get("/api/menu/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Dish not found: 99")));
    }

    // ===== POST =====
    @Test
    void platoMenuController_crear_retorna201() throws Exception {
        // Arrange
        PlatoMenuRequest solicitud = new PlatoMenuRequest("Buddha Bowl", "Quinoa and roasted vegetables.",
                new BigDecimal("24.50"), RestauranteHotel.ROOM_SERVICE, CategoriaPlato.PRINCIPAL,
                List.of(EtiquetaPlato.VEGANO, EtiquetaPlato.SIN_GLUTEN), true, null);
        when(platoMenuService.crear(any(PlatoMenuRequest.class))).thenReturn(crearPlato(10L, "Buddha Bowl"));

        // Act & Assert
        mockMvc.perform(post("/api/menu")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitud)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(10)));
        verify(platoMenuService).crear(eq(solicitud));
    }

    @Test
    void platoMenuController_crear_datosInvalidos_retorna400() throws Exception {
        // Arrange
        when(platoMenuService.crear(any(PlatoMenuRequest.class)))
                .thenThrow(new IllegalArgumentException("Price must be greater than 0."));

        // Act & Assert
        mockMvc.perform(post("/api/menu")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Bowl\",\"descripcion\":\"x\",\"precio\":0,"
                                + "\"restaurante\":\"ROOM_SERVICE\",\"categoria\":\"PRINCIPAL\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Price must be greater than 0.")));
    }

    // ===== PUT =====
    @Test
    void platoMenuController_actualizar_retorna200() throws Exception {
        // Arrange
        when(platoMenuService.actualizar(eq(3L), any(PlatoMenuRequest.class)))
                .thenReturn(crearPlato(3L, "Buddha Bowl Deluxe"));

        // Act & Assert
        mockMvc.perform(put("/api/menu/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Buddha Bowl Deluxe\",\"descripcion\":\"Bigger bowl.\",\"precio\":24.5,"
                                + "\"restaurante\":\"ROOM_SERVICE\",\"categoria\":\"PRINCIPAL\","
                                + "\"etiquetas\":[\"VEGANO\"],\"disponible\":true,\"imagenUrl\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre", is("Buddha Bowl Deluxe")));
    }

    // ===== DELETE =====
    @Test
    void platoMenuController_eliminar_retorna204() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/menu/3"))
                .andExpect(status().isNoContent());
        verify(platoMenuService).eliminar(3L);
    }

    @Test
    void platoMenuController_eliminar_inexistente_retorna404() throws Exception {
        // Arrange
        doThrow(new RecursoNoEncontradoException("Dish not found: 8")).when(platoMenuService).eliminar(8L);

        // Act & Assert
        mockMvc.perform(delete("/api/menu/8"))
                .andExpect(status().isNotFound());
    }

    private PlatoMenu crearPlato(Long id, String nombre) {
        return PlatoMenu.builder()
                .id(id)
                .nombre(nombre)
                .descripcion("Quinoa and roasted vegetables.")
                .precio(new BigDecimal("24.50"))
                .restaurante(RestauranteHotel.ROOM_SERVICE)
                .categoria(CategoriaPlato.PRINCIPAL)
                .etiquetas(new LinkedHashSet<>(List.of(EtiquetaPlato.SIN_GLUTEN, EtiquetaPlato.VEGANO)))
                .disponible(true)
                .build();
    }
}
