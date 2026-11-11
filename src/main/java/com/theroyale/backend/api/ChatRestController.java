package com.theroyale.backend.api;

import com.theroyale.backend.dto.ChatRequest;
import com.theroyale.backend.dto.EstadoChatResponse;
import com.theroyale.backend.service.chat.ChatService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

// ===== API del chatbot "Royale Concierge", consumida por chat.service.ts del frontend =====
// POST /api/chat responde text/event-stream con los eventos texto, tarjetas, sugerencias y fin (o error).
// Los errores previos al stream (400, 429, 503) salen como JSON {status, message} vía ApiExceptionHandler.
// La clave de Gemini nunca llega al navegador: el front solo habla con este controlador.
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "http://localhost:4200")
public class ChatRestController {

    @Autowired
    private ChatService chatService;

    @GetMapping("/estado")
    public EstadoChatResponse estado() {
        return new EstadoChatResponse(chatService.estaDisponible());
    }

    // La IP del cliente se usa solo para el límite de mensajes (no se guarda ni se envía a Gemini)
    @PostMapping
    public SseEmitter conversar(@RequestBody ChatRequest solicitud, HttpServletRequest peticion) {
        return chatService.iniciarConversacion(solicitud, peticion.getRemoteAddr());
    }
}
