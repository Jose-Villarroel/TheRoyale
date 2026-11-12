package com.theroyale.backend.api;

import com.theroyale.backend.dto.ChatRequest;
import com.theroyale.backend.dto.TurnoChatRequest;
import com.theroyale.backend.errors.LimiteExcedidoException;
import com.theroyale.backend.errors.ServicioNoDisponibleException;
import com.theroyale.backend.service.chat.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ===== Pruebas de ChatRestController con mocks =====
// @WebMvcTest levanta solo el controlador y ApiExceptionHandler; ChatService se simula.
// Se comprueba el stream SSE (Content-Type y eventos con nombre) y que los errores previos al
// stream salgan como JSON {status, message} aunque el navegador pida text/event-stream.
@WebMvcTest(ChatRestController.class)
@ActiveProfiles("test")
class ChatRestControllerTest {

    private static final String CUERPO = "{\"mensaje\":\"Hello\",\"historial\":[{\"rol\":\"usuario\",\"texto\":\"Hi\"}]}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatService chatService;

    // ===== GET /api/chat/estado =====
    @Test
    void chatController_estado_sinClave_retornaDisponibleFalse() throws Exception {
        // Arrange
        when(chatService.estaDisponible()).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/api/chat/estado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible", is(false)));
    }

    // ===== POST /api/chat: stream SSE =====
    @Test
    void chatController_conversar_retornaEventStreamConEventosConNombre() throws Exception {
        // Arrange: el emisor ya trae los eventos y está completo (SseEmitter los guarda hasta conectarse)
        SseEmitter emisor = new SseEmitter();
        emisor.send(SseEmitter.event().name("texto").data(Map.of("fragmento", "Welcome!"), MediaType.APPLICATION_JSON));
        emisor.send(SseEmitter.event().name("sugerencias")
                .data(Map.of("sugerencias", List.of("Rooms", "Dining")), MediaType.APPLICATION_JSON));
        emisor.send(SseEmitter.event().name("fin").data(Map.of(), MediaType.APPLICATION_JSON));
        emisor.complete();
        when(chatService.iniciarConversacion(eq(new ChatRequest("Hello",
                List.of(new TurnoChatRequest("usuario", "Hi")))), anyString()))
                .thenReturn(emisor);

        // Act
        MvcResult resultado = mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content(CUERPO))
                .andExpect(request().asyncStarted())
                .andReturn();

        // Assert
        mockMvc.perform(asyncDispatch(resultado))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(containsString("event:texto\ndata:{\"fragmento\":\"Welcome!\"}")))
                .andExpect(content().string(containsString("event:sugerencias\ndata:{\"sugerencias\":[\"Rooms\",\"Dining\"]}")))
                .andExpect(content().string(containsString("event:fin\ndata:{}")));
    }

    // ===== Errores antes del stream: JSON aunque se pida text/event-stream =====
    @Test
    void chatController_conversar_mensajeInvalido_retorna400EnJson() throws Exception {
        // Arrange
        when(chatService.iniciarConversacion(any(), anyString()))
                .thenThrow(new IllegalArgumentException("The message cannot be empty."));

        // Act & Assert
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"mensaje\":\"\",\"historial\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("The message cannot be empty.")));
    }

    @Test
    void chatController_conversar_cuerpoMalFormado_retorna400() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    void chatController_conversar_limiteExcedido_retorna429() throws Exception {
        // Arrange
        when(chatService.iniciarConversacion(any(), anyString()))
                .thenThrow(new LimiteExcedidoException("Too many messages, please try again in a few minutes."));

        // Act & Assert
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content(CUERPO))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status", is(429)))
                .andExpect(jsonPath("$.message", is("Too many messages, please try again in a few minutes.")));
    }

    @Test
    void chatController_conversar_sinClave_retorna503() throws Exception {
        // Arrange
        when(chatService.iniciarConversacion(any(), anyString()))
                .thenThrow(new ServicioNoDisponibleException("The concierge is offline right now."));

        // Act & Assert
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content(CUERPO))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status", is(503)))
                .andExpect(jsonPath("$.message", is("The concierge is offline right now.")));
    }
}
