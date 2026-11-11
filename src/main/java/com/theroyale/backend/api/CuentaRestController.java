package com.theroyale.backend.api;

import com.theroyale.backend.dto.AgregarConsumoRequest;
import com.theroyale.backend.dto.CuentaResponse;
import com.theroyale.backend.dto.RegistrarPagoRequest;
import com.theroyale.backend.service.CuentaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// ===== API REST de la cuenta de consumo de una reserva, consumida por el portal del operador =====
// Equivale a los POST de OperatorController, que siguen sirviendo a las vistas Thymeleaf.
// Las reglas viven en CuentaService; aqui solo se exponen. ApiExceptionHandler traduce
// IllegalState -> 409 (cuenta cerrada, cargo ya pagado, pago mayor al saldo) e IllegalArgument -> 400.
@RestController
@RequestMapping("/api/reservas/{reservaId}/cuenta")
@CrossOrigin(origins = "http://localhost:4200")
public class CuentaRestController {

    @Autowired
    private CuentaService cuentaService;

    @GetMapping
    public CuentaResponse obtener(@PathVariable Long reservaId) {
        return cuentaService.obtenerRespuestaPorReserva(reservaId);
    }

    @PostMapping("/consumos")
    public CuentaResponse agregarConsumo(@PathVariable Long reservaId,
                                         @RequestBody AgregarConsumoRequest solicitud) {
        return cuentaService.agregarConsumo(reservaId, solicitud.servicioId(),
                solicitud.cantidad(), solicitud.operadorId());
    }

    @DeleteMapping("/consumos/{itemId}")
    public CuentaResponse eliminarConsumo(@PathVariable Long reservaId, @PathVariable Long itemId) {
        return cuentaService.eliminarConsumo(reservaId, itemId);
    }

    @PostMapping("/pagos")
    public CuentaResponse registrarPago(@PathVariable Long reservaId,
                                        @RequestBody RegistrarPagoRequest solicitud) {
        return cuentaService.registrarPago(reservaId, solicitud.monto(),
                solicitud.metodoPago(), solicitud.operadorId());
    }
}
