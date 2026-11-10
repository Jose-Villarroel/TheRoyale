package com.theroyale.backend.api;

import com.theroyale.backend.dto.ChatRequest;
import com.theroyale.backend.dto.ChatResponse;
import com.theroyale.backend.dto.ItinerarioRequest;
import com.theroyale.backend.dto.ItinerarioResponse;
import com.theroyale.backend.service.ConciergeAiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// ===== API REST para el Chatbot Concierge e Itinerarios con IA =====
@RestController
@RequestMapping("/api/concierge")
@CrossOrigin(origins = "http://localhost:4200")
public class ConciergeRestController {

    @Autowired
    private ConciergeAiService conciergeAiService;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        return ResponseEntity.ok(conciergeAiService.procesarMensaje(request));
    }

    @PostMapping("/itinerario")
    public ResponseEntity<ItinerarioResponse> generarItinerario(@RequestBody ItinerarioRequest request) {
        return ResponseEntity.ok(conciergeAiService.generarItinerario(request));
    }
}
