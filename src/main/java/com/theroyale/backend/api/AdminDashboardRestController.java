package com.theroyale.backend.api;

import com.theroyale.backend.dto.dashboard.AlertaResponse;
import com.theroyale.backend.dto.dashboard.DesgloseResponse;
import com.theroyale.backend.dto.dashboard.OperacionHoyResponse;
import com.theroyale.backend.dto.dashboard.PuntoIngresosResponse;
import com.theroyale.backend.dto.dashboard.PuntoOcupacionResponse;
import com.theroyale.backend.dto.dashboard.ReputacionResponse;
import com.theroyale.backend.dto.dashboard.ResumenResponse;
import com.theroyale.backend.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

// ===== API REST del dashboard de dirección, consumida por dashboard.service.ts del frontend =====
//
// Seguridad: SecurityConfig ya exige hasAuthority("ROLE_ADMIN") para todo /api/admin/**. El
// @PreAuthorize de esta clase es una segunda barrera a propósito: si alguien reordena o afloja los
// matchers de SecurityConfig, estos datos (ingresos, caja, saldos de los huéspedes) no quedan
// expuestos por accidente. Sin él, /api/admin/** caería en anyRequest().authenticated() y
// cualquier cliente u operador con sesión podría leer las finanzas del hotel.
//
// No lleva @CrossOrigin: desde el sprint de seguridad el CORS es central en security/CorsConfig.
//
// Las fechas son opcionales en todos los endpoints; sin ellas se asumen los últimos 30 días, que es
// lo que el panel pide al abrirse. Las validaciones del período viven en DashboardService y salen
// como 400 por ApiExceptionHandler.
@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminDashboardRestController {

    @Autowired
    private DashboardService dashboardService;

    // KPIs del período y del anterior equivalente, para la fila de tarjetas
    @GetMapping("/resumen")
    public ResumenResponse resumen(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return dashboardService.resumen(desdeO(desde), hastaO(hasta));
    }

    // Serie de ingresos: habitaciones y servicios apilados, con el cobrado como línea
    @GetMapping("/ingresos-serie")
    public List<PuntoIngresosResponse> ingresosSerie(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false, defaultValue = "dia") String agrupacion) {
        return dashboardService.ingresosSerie(desdeO(desde), hastaO(hasta), agrupacion);
    }

    // Ocupación por día; siempre llega hasta 30 días en el futuro con lo ya reservado
    @GetMapping("/ocupacion-serie")
    public List<PuntoOcupacionResponse> ocupacionSerie(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return dashboardService.ocupacionSerie(desdeO(desde), hastaO(hasta));
    }

    // Operación del día y mapa de habitaciones. No recibe fechas: hoy es hoy.
    @GetMapping("/hoy")
    public OperacionHoyResponse hoy() {
        return dashboardService.operacionHoy();
    }

    // Ingresos y actividad por tipo de habitación, servicio, método de pago y operador
    @GetMapping("/desglose")
    public DesgloseResponse desglose(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return dashboardService.desglose(desdeO(desde), hastaO(hasta));
    }

    // Reputación: rating global, distribución de estrellas, reseñas recientes y tipo de huésped
    @GetMapping("/reputacion")
    public ReputacionResponse reputacion(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return dashboardService.reputacion(desdeO(desde), hastaO(hasta));
    }

    // Alertas accionables, ya ordenadas por severidad
    @GetMapping("/alertas")
    public List<AlertaResponse> alertas() {
        return dashboardService.alertas();
    }

    private LocalDate desdeO(LocalDate desde) {
        return desde == null ? dashboardService.desdePorDefecto() : desde;
    }

    private LocalDate hastaO(LocalDate hasta) {
        return hasta == null ? dashboardService.hastaPorDefecto() : hasta;
    }
}
