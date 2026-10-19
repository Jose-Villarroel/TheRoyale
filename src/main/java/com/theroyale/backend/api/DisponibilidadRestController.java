package com.theroyale.backend.api;

import com.theroyale.backend.dto.DisponibilidadTipoResponse;
import com.theroyale.backend.service.ReservaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

// ===== API REST consumida por disponibilidad.service.ts del frontend Angular =====
// Va en su propio controlador: bajo /api/tipos-habitacion chocaria con el @GetMapping("/{id}").
@RestController
@RequestMapping("/api/disponibilidad")
@CrossOrigin(origins = "http://localhost:4200")
public class DisponibilidadRestController {

    @Autowired
    private ReservaService reservaService;

    // ===== Habitaciones libres de cada tipo entre dos fechas =====
    @GetMapping
    public List<DisponibilidadTipoResponse> consultar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return reservaService.consultarDisponibilidad(fechaInicio, fechaFin);
    }
}
