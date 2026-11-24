package com.theroyale.backend.api;

import com.theroyale.backend.dto.PronosticoOcupacionResponse;
import com.theroyale.backend.service.ocupacion.PronosticoOcupacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// ===== API del pronóstico de ocupación (solo admin: /api/admin/** en SecurityConfig) =====
// Consumida por ocupacion.service.ts del panel de administración.
@RestController
@RequestMapping("/api/admin/ocupacion")
@CrossOrigin(origins = "http://localhost:4200")
public class OcupacionRestController {

    @Autowired
    private PronosticoOcupacionService pronosticoOcupacionService;

    // dias: entre 7 y 90 (400 si no). tipoId: sin él, el hotel completo.
    @GetMapping
    public PronosticoOcupacionResponse pronostico(@RequestParam(defaultValue = "90") int dias,
                                                  @RequestParam(required = false) Long tipoId) {
        return pronosticoOcupacionService.pronostico(dias, tipoId);
    }
}
