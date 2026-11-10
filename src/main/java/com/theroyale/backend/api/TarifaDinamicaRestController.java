package com.theroyale.backend.api;

import com.theroyale.backend.dto.DetalleTarifaResponse;
import com.theroyale.backend.service.TarifaDinamicaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

// ===== API REST para el Motor de Precios Dinámicos =====
@RestController
@RequestMapping("/api/tarifas")
@CrossOrigin(origins = "http://localhost:4200")
public class TarifaDinamicaRestController {

    @Autowired
    private TarifaDinamicaService tarifaDinamicaService;

    @GetMapping("/estimar")
    public ResponseEntity<DetalleTarifaResponse> estimarTarifa(
            @RequestParam("tipoId") Long tipoId,
            @RequestParam("fechaInicio") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam("fechaFin") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return ResponseEntity.ok(tarifaDinamicaService.calcularDetalleTarifa(tipoId, fechaInicio, fechaFin));
    }

    @GetMapping("/estado")
    public ResponseEntity<Map<String, Object>> obtenerEstado() {
        return ResponseEntity.ok(tarifaDinamicaService.obtenerEstadoMotor());
    }

    @PostMapping("/toggle")
    public ResponseEntity<Map<String, Object>> toggleMotor(@RequestBody Map<String, Boolean> payload) {
        Boolean activo = payload.getOrDefault("activo", true);
        tarifaDinamicaService.setMotorActivo(activo);
        return ResponseEntity.ok(Map.of("activo", tarifaDinamicaService.isMotorActivo(), "mensaje", "Motor de precios dinámicos actualizado"));
    }
}
