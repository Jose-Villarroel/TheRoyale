package com.theroyale.backend.errors;

import com.theroyale.backend.api.ResenaRestController;
import com.theroyale.backend.config.I18nConfig;
import com.theroyale.backend.security.UsuarioActualService;
import com.theroyale.backend.service.ResenaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ===== El idioma de la respuesta de error lo decide Accept-Language; el código nunca cambia =====
// Contrato: docs/i18n-codigos-error.md. Se prueba de punta a punta (MockMvc -> I18nConfig ->
// ApiExceptionHandler -> MessageSource) con GET /api/resenas sin filtros, que es el 400 más simple
// del backend (resena.filtro-requerido) y no necesita autenticación ni datos sembrados.
//
// @Import(I18nConfig.class): @WebMvcTest no incluye las clases @Configuration del paquete config, y
// sin el AcceptHeaderLocaleResolver de ahí "Accept-Language: fr" resolvería a francés en vez de caer
// al inglés, que es justo lo que esta prueba vigila.
@WebMvcTest(controllers = ResenaRestController.class)
@Import(I18nConfig.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ApiExceptionHandlerI18nTest {

    private static final String CODIGO_FILTRO = "resena.filtro-requerido";
    private static final String EN_FILTRO = "Specify tipoHabitacionId, clienteId or incluirOcultas=true.";
    private static final String ES_FILTRO = "Indique tipoHabitacionId, clienteId o incluirOcultas=true.";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResenaService resenaService;

    @MockBean
    private UsuarioActualService usuarioActualService;

    // ===== (a) Accept-Language: es -> mensaje en español y EL MISMO código =====
    @Test
    void apiExceptionHandler_acceptLanguageEs_devuelveElMensajeEnEspanolYElMismoCodigo() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/resenas").header(HttpHeaders.ACCEPT_LANGUAGE, "es"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.codigo", is(CODIGO_FILTRO)))
                .andExpect(jsonPath("$.message", is(ES_FILTRO)));
    }

    // Cualquier variante regional ("es-CO", "es-419") es español
    @Test
    void apiExceptionHandler_acceptLanguageEsCo_tambienDevuelveEspanol() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/resenas").header(HttpHeaders.ACCEPT_LANGUAGE, "es-CO,es;q=0.9"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo", is(CODIGO_FILTRO)))
                .andExpect(jsonPath("$.message", is(ES_FILTRO)));
    }

    // ===== (b) Un idioma que no se soporta cae al inglés, no al idioma pedido =====
    @Test
    void apiExceptionHandler_acceptLanguageFr_caeAlIngles() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/resenas").header(HttpHeaders.ACCEPT_LANGUAGE, "fr"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo", is(CODIGO_FILTRO)))
                .andExpect(jsonPath("$.message", is(EN_FILTRO)));
    }

    // ===== (c) Sin Accept-Language -> inglés =====
    // Esto es lo que protege spring.messages.fallback-to-system-locale=false: sin esa línea, un
    // servidor con el sistema operativo en español respondería en español aunque no llegue el header.
    @Test
    void apiExceptionHandler_sinAcceptLanguage_devuelveIngles() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/resenas"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo", is(CODIGO_FILTRO)))
                .andExpect(jsonPath("$.message", is(EN_FILTRO)));
    }

    // ===== Los parámetros del mensaje se ven igual en los dos idiomas =====
    // MessageFormat formatea los números según el idioma: un {0} con 1000 saldría "1,000" en inglés
    // y "1.000" en español. ApiExceptionHandler los pasa como texto para evitarlo.
    @Test
    void apiExceptionHandler_mensajeConParametros_losNumerosNoSeFormateanPorIdioma() throws Exception {
        // Arrange
        when(resenaService.listarPorCliente(8L))
                .thenThrow(ReglaNegocioException.badRequest("resena.comentario-longitud", 10, 1000));

        // Act & Assert
        mockMvc.perform(get("/api/resenas").param("clienteId", "8")
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "es"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo", is("resena.comentario-longitud")))
                .andExpect(jsonPath("$.message",
                        is("El comentario es obligatorio y debe tener entre 10 y 1000 caracteres.")));

        mockMvc.perform(get("/api/resenas").param("clienteId", "8"))
                .andExpect(jsonPath("$.message",
                        is("The comment is required and must be between 10 and 1000 characters.")));
    }

    // ===== Lo que no se migra responde codigo: null y conserva su literal en inglés =====
    // Son los errores que solo alcanzan el panel de administración o el portal del operador.
    @Test
    void apiExceptionHandler_excepcionSinCodigo_respondeCodigoNullYSuLiteralAunqueSePidaEspanol() throws Exception {
        // Arrange
        when(resenaService.listarPorCliente(8L))
                .thenThrow(new RecursoNoEncontradoException("Client not found: 8"));

        // Act & Assert
        mockMvc.perform(get("/api/resenas").param("clienteId", "8")
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "es"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo", is(nullValue())))
                .andExpect(jsonPath("$.message", is("Client not found: 8")));
    }

    // ===== El estado viaja en la excepción, no en su tipo =====
    @Test
    void apiExceptionHandler_reglaNegocioConflict_responde409ConSuCodigoTraducido() throws Exception {
        // Arrange
        when(resenaService.listarPorCliente(8L)).thenThrow(ReglaNegocioException.conflict("resena.ya-existe"));

        // Act & Assert
        mockMvc.perform(get("/api/resenas").param("clienteId", "8")
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "es"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.codigo", is("resena.ya-existe")))
                .andExpect(jsonPath("$.message", is("Esta reserva ya tiene una reseña.")));
    }
}
