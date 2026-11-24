package com.theroyale.backend.api;

import com.theroyale.backend.dto.PreciosDtos.EstadoRecomendacionesResponse;
import com.theroyale.backend.dto.PreciosDtos.RecomendacionResponse;
import com.theroyale.backend.dto.PreciosDtos.TarifaResponse;
import com.theroyale.backend.dto.PreciosDtos.TemporadaResponse;
import com.theroyale.backend.dto.TemporadaRequest;
import com.theroyale.backend.model.EstadoRecomendacion;
import com.theroyale.backend.security.UsuarioActualService;
import com.theroyale.backend.service.precio.RecomendacionPrecioService;
import com.theroyale.backend.service.precio.TemporadaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ===== API de precios dinámicos para el admin (solo admin: /api/admin/** en SecurityConfig) =====
// El sistema recomienda y el admin decide: aplicar una recomendación es lo único que cambia el
// precio que pagan las reservas nuevas. Consumida por precio.service.ts del panel de administración.
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:4200")
public class PrecioRestController {

    @Autowired
    private TemporadaService temporadaService;

    @Autowired
    private RecomendacionPrecioService recomendacionService;

    @Autowired
    private UsuarioActualService usuarioActualService;

    // ===== Temporadas =====
    @GetMapping("/temporadas")
    public List<TemporadaResponse> listarTemporadas() {
        return temporadaService.listar().stream().map(TemporadaResponse::from).toList();
    }

    @PostMapping("/temporadas")
    public ResponseEntity<TemporadaResponse> crearTemporada(@RequestBody TemporadaRequest solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(TemporadaResponse.from(temporadaService.crear(solicitud)));
    }

    @PutMapping("/temporadas/{id}")
    public TemporadaResponse actualizarTemporada(@PathVariable Long id, @RequestBody TemporadaRequest solicitud) {
        return TemporadaResponse.from(temporadaService.actualizar(id, solicitud));
    }

    @DeleteMapping("/temporadas/{id}")
    public ResponseEntity<Void> eliminarTemporada(@PathVariable Long id) {
        temporadaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // ===== Recomendaciones =====
    // Si la última generación tiene más de 7 días (o nunca hubo), se genera antes de responder
    @GetMapping("/precios/recomendaciones")
    public List<RecomendacionResponse> listarRecomendaciones(@RequestParam(required = false) EstadoRecomendacion estado) {
        recomendacionService.generarSiCorresponde();
        return recomendacionService.listar(estado).stream().map(RecomendacionResponse::from).toList();
    }

    @GetMapping("/precios/estado")
    public EstadoRecomendacionesResponse estado() {
        return new EstadoRecomendacionesResponse(recomendacionService.ultimaGeneracion().orElse(null),
                recomendacionService.proximaGeneracion(), recomendacionService.pendientes());
    }

    @PostMapping("/precios/recomendaciones/recalcular")
    public List<RecomendacionResponse> recalcular() {
        recomendacionService.generar();
        return recomendacionService.listar(EstadoRecomendacion.PENDIENTE).stream().map(RecomendacionResponse::from).toList();
    }

    @PostMapping("/precios/recomendaciones/{id}/aplicar")
    public RecomendacionResponse aplicar(@PathVariable Long id) {
        return RecomendacionResponse.from(recomendacionService.aplicar(id, usuarioActualService.obtenerEmail()));
    }

    @PostMapping("/precios/recomendaciones/{id}/descartar")
    public RecomendacionResponse descartar(@PathVariable Long id) {
        return RecomendacionResponse.from(recomendacionService.descartar(id, usuarioActualService.obtenerEmail()));
    }

    // Aplica todas las pendientes de un tipo de habitación
    @PostMapping("/precios/recomendaciones/aplicar")
    public List<RecomendacionResponse> aplicarTipo(@RequestParam Long tipoId) {
        return recomendacionService.aplicarTipo(tipoId, usuarioActualService.obtenerEmail()).stream()
                .map(RecomendacionResponse::from).toList();
    }

    // ===== Tarifas aprobadas =====
    @GetMapping("/precios/tarifas")
    public List<TarifaResponse> tarifas() {
        return recomendacionService.tarifas().stream().map(TarifaResponse::from).toList();
    }

    @DeleteMapping("/precios/tarifas/{id}")
    public ResponseEntity<Void> revertirTarifa(@PathVariable Long id) {
        recomendacionService.revertirTarifa(id);
        return ResponseEntity.noContent().build();
    }
}
