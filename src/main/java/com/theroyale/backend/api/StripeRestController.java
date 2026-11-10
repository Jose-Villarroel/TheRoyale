package com.theroyale.backend.api;

import com.theroyale.backend.dto.ConfirmarStripeRequest;
import com.theroyale.backend.dto.CuentaResponse;
import com.theroyale.backend.dto.StripeIntentRequest;
import com.theroyale.backend.dto.StripeIntentResponse;
import com.theroyale.backend.service.StripePaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// ===== API REST para Pasarela de Pagos Stripe =====
@RestController
@RequestMapping("/api/pagos/stripe")
@CrossOrigin(origins = "http://localhost:4200")
public class StripeRestController {

    @Autowired
    private StripePaymentService stripePaymentService;

    @PostMapping("/crear-intent")
    public ResponseEntity<StripeIntentResponse> crearIntent(@RequestBody StripeIntentRequest request) {
        return ResponseEntity.ok(stripePaymentService.crearIntent(request));
    }

    @PostMapping("/confirmar")
    public ResponseEntity<CuentaResponse> confirmarPago(@RequestBody ConfirmarStripeRequest request) {
        return ResponseEntity.ok(stripePaymentService.confirmarPago(request));
    }
}
