package com.theroyale.backend.api;

import com.theroyale.backend.dto.CrearReservaRequest;
import com.theroyale.backend.model.Reserva;
import com.theroyale.backend.service.ReservaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// ===== API REST consumida por reserva.service.ts del frontend Angular =====
@RestController
@RequestMapping("/api/reservas")
@CrossOrigin(origins = "http://localhost:4200")
public class ReservaRestController {

    @Autowired
    private ReservaService reservaService;

    // ===== Crea una reserva: el sistema asigna una habitación libre del tipo pedido =====
    @PostMapping
    public ResponseEntity<Reserva> crear(@RequestBody CrearReservaRequest solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reservaService.crear(solicitud));
    }
}