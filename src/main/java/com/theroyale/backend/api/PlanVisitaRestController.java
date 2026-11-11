package com.theroyale.backend.api;

import com.theroyale.backend.dto.CuentaResponse;
import com.theroyale.backend.dto.plan.AgregarBloqueRequest;
import com.theroyale.backend.dto.plan.AjustarPlanRequest;
import com.theroyale.backend.dto.plan.EstadoPlanResponse;
import com.theroyale.backend.dto.plan.GenerarPlanRequest;
import com.theroyale.backend.dto.plan.InterpretacionPlan;
import com.theroyale.backend.dto.plan.InterpretarPlanRequest;
import com.theroyale.backend.dto.plan.PlanVisitaResponse;
import com.theroyale.backend.dto.plan.RegenerarDiaRequest;
import com.theroyale.backend.service.plan.PlanVisitaService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

// ===== API del plan de visita con IA, consumida por plan-visita.service.ts del frontend =====
// Las rutas cuelgan de la reserva, como CuentaRestController. Mientras no haya JWT, clienteId viaja
// en la petición y PlanVisitaService valida que la reserva sea suya (403). Los errores salen como
// { status, message } por ApiExceptionHandler: 400, 403, 404, 409, 429, 502 y 503.
@RestController
@CrossOrigin(origins = "http://localhost:4200")
public class PlanVisitaRestController {

    @Autowired
    private PlanVisitaService planVisitaService;

    // false si no hay GEMINI_API_KEY: el front muestra "The planner is unavailable right now"
    @GetMapping("/api/plan/estado")
    public EstadoPlanResponse estado() {
        return new EstadoPlanResponse(planVisitaService.estaDisponible());
    }

    // La IP solo se usa para el límite de mensajes por IP (no se guarda ni se envía a Gemini)
    @PostMapping("/api/reservas/{reservaId}/plan/interpretar")
    public InterpretacionPlan interpretar(@PathVariable Long reservaId, @RequestBody InterpretarPlanRequest solicitud,
                                          HttpServletRequest peticion) {
        return planVisitaService.interpretar(reservaId, solicitud, peticion.getRemoteAddr());
    }

    @PostMapping("/api/reservas/{reservaId}/plan")
    public ResponseEntity<PlanVisitaResponse> generar(@PathVariable Long reservaId,
                                                      @RequestBody GenerarPlanRequest solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planVisitaService.generar(reservaId, solicitud));
    }

    @GetMapping("/api/reservas/{reservaId}/plan")
    public PlanVisitaResponse obtener(@PathVariable Long reservaId, @RequestParam Long clienteId) {
        return planVisitaService.obtener(reservaId, clienteId);
    }

    @PostMapping("/api/reservas/{reservaId}/plan/ajustar")
    public PlanVisitaResponse ajustar(@PathVariable Long reservaId, @RequestBody AjustarPlanRequest solicitud) {
        return planVisitaService.ajustar(reservaId, solicitud);
    }

    @PostMapping("/api/reservas/{reservaId}/plan/dias/{fecha}/regenerar")
    public PlanVisitaResponse regenerarDia(@PathVariable Long reservaId,
                                           @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                           @RequestBody RegenerarDiaRequest solicitud) {
        return planVisitaService.regenerarDia(reservaId, fecha, solicitud);
    }

    @PostMapping("/api/reservas/{reservaId}/plan/bloques/{bloqueId}/agregar-a-cuenta")
    public ResponseEntity<CuentaResponse> agregarACuenta(@PathVariable Long reservaId, @PathVariable String bloqueId,
                                                         @RequestBody AgregarBloqueRequest solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(planVisitaService.agregarACuenta(reservaId, bloqueId, solicitud));
    }

    @DeleteMapping("/api/reservas/{reservaId}/plan")
    public ResponseEntity<Void> eliminar(@PathVariable Long reservaId, @RequestParam Long clienteId) {
        planVisitaService.eliminar(reservaId, clienteId);
        return ResponseEntity.noContent().build();
    }
}
