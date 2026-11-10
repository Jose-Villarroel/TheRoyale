package com.theroyale.backend.service;

import com.theroyale.backend.dto.ConfirmarStripeRequest;
import com.theroyale.backend.dto.CuentaResponse;
import com.theroyale.backend.dto.StripeIntentRequest;
import com.theroyale.backend.dto.StripeIntentResponse;
import com.theroyale.backend.errors.RecursoNoEncontradoException;
import com.theroyale.backend.model.Cuenta;
import com.theroyale.backend.model.EstadoCuenta;
import com.theroyale.backend.model.Reserva;
import com.theroyale.backend.repository.CuentaRepository;
import com.theroyale.backend.repository.ReservaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// ===== Servicio de Integración con Pasarela de Pagos Stripe =====
@Service
public class StripePaymentService {

    @Autowired
    private CuentaService cuentaService;

    @Autowired
    private CuentaRepository cuentaRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    private static final String STRIPE_PUBLISHABLE_KEY = "pk_test_theroyale_manhattan_51Oz9A";

    public StripeIntentResponse crearIntent(StripeIntentRequest request) {
        String paymentIntentId = "pi_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        String clientSecret = paymentIntentId + "_secret_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        BigDecimal monto = request.monto() != null ? request.monto() : BigDecimal.valueOf(100.0);
        String moneda = request.moneda() != null ? request.moneda().toLowerCase() : "usd";

        return new StripeIntentResponse(
                clientSecret,
                paymentIntentId,
                monto,
                moneda,
                "requires_payment_method",
                STRIPE_PUBLISHABLE_KEY
        );
    }

    @Transactional
    public CuentaResponse confirmarPago(ConfirmarStripeRequest request) {
        Reserva reserva = reservaRepository.findById(request.reservaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada con id: " + request.reservaId()));

        // Asegurar que la reserva tenga una cuenta creada
        cuentaRepository.findByReservaId(request.reservaId()).orElseGet(() -> {
            Cuenta nueva = Cuenta.builder()
                    .reserva(reserva)
                    .estado(EstadoCuenta.ABIERTA)
                    .fechaCreacion(LocalDateTime.now())
                    .build();
            return cuentaRepository.save(nueva);
        });

        // Registrar pago vía STRIPE
        return cuentaService.registrarPago(request.reservaId(), request.monto(), "STRIPE", null);
    }
}
