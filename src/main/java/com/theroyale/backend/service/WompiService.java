package com.theroyale.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theroyale.backend.dto.CrearReservaRequest;
import com.theroyale.backend.dto.ReservaResponse;
import com.theroyale.backend.dto.wompi.WompiPrepararRequest;
import com.theroyale.backend.dto.wompi.WompiPrepararResponse;
import com.theroyale.backend.dto.wompi.WompiVerificarResponse;
import com.theroyale.backend.errors.RecursoNoEncontradoException;
import com.theroyale.backend.model.Cuenta;
import com.theroyale.backend.model.EstadoCuenta;
import com.theroyale.backend.model.EstadoReserva;
import com.theroyale.backend.model.Pago;
import com.theroyale.backend.model.Reserva;
import com.theroyale.backend.repository.CuentaRepository;
import com.theroyale.backend.repository.ReservaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class WompiService {

    @Value("${wompi.api.url:https://sandbox.wompi.co/v1}")
    private String wompiApiUrl;

    @Value("${wompi.public-key}")
    private String publicKey;

    @Value("${wompi.private-key}")
    private String privateKey;

    @Value("${wompi.integrity-secret}")
    private String integritySecret;

    @Value("${wompi.currency:COP}")
    private String currency;

    @Value("${wompi.tasa-cambio-cop:4000}")
    private double tasaCambioCop;

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private CuentaRepository cuentaRepository;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WompiService.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Calcula la firma de integridad SHA-256 exigida por Wompi:
     * SHA256(reference + amountInCents + currency + integritySecret)
     */
    public String calcularFirmaIntegridad(String reference, long amountInCents, String currency) {
        String cadena = reference.trim() + amountInCents + currency.trim() + integritySecret.trim();
        log.info("Wompi - Cadena a firmar: {}", cadena);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(cadena.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            String firma = hexString.toString();
            log.info("Wompi - Firma generada: {}", firma);
            return firma;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error calculando hash SHA-256 para Wompi", e);
        }
    }

    /**
     * Paso 1: Crea la reserva en estado PENDIENTE, calcula montos y genera la firma de Wompi
     */
    @Transactional
    public WompiPrepararResponse prepararPago(WompiPrepararRequest request) {
        if (request == null || request.clienteId() == null || request.tipoHabitacionId() == null) {
            throw new IllegalArgumentException("Datos incompletos para preparar la reserva.");
        }

        // 1. Crear reserva inicial en estado PENDIENTE
        CrearReservaRequest solicitud = new CrearReservaRequest(
                request.clienteId(),
                request.tipoHabitacionId(),
                request.fechaInicio(),
                request.fechaFin(),
                request.cantidadPersonas()
        );
        Reserva reserva = reservaService.crear(solicitud);

        // 2. Calcular noches y montos
        long noches = Math.max(1, ChronoUnit.DAYS.between(reserva.getFechaInicio(), reserva.getFechaFin()));
        double totalUsd = reserva.getPrecioNocheAcordado().doubleValue() * noches;
        double totalCop = totalUsd * tasaCambioCop;
        long amountInCents = Math.round(totalCop * 100);

        // 3. Generar referencia única de pago
        String reference = "ROYALE-" + reserva.getId() + "-" + System.currentTimeMillis();

        // 4. Generar firma de integridad SHA-256
        String signature = calcularFirmaIntegridad(reference, amountInCents, currency);

        String email = reserva.getCliente() != null ? reserva.getCliente().getEmail() : "";
        String nombre = reserva.getCliente() != null ? reserva.getCliente().getNombreCompleto() : "";

        return new WompiPrepararResponse(
                reserva.getId(),
                reference,
                amountInCents,
                totalUsd,
                totalCop,
                currency,
                signature,
                publicKey.trim(),
                email,
                nombre
        );
    }

    /**
     * Paso 2: Consulta a la API oficial de Wompi para validar que la transacción esté APPROVED,
     * registra el pago y confirma la reserva.
     */
    @Transactional
    public WompiVerificarResponse verificarYConfirmar(String transactionId, Long reservaId) {
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("El transactionId de Wompi es requerido.");
        }

        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada: " + reservaId));

        try {
            // Consultar transacción directamente a la API de Wompi
            String url = wompiApiUrl + "/transactions/" + transactionId.trim();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(privateKey.trim());
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return new WompiVerificarResponse(false, "ERROR", "No se pudo consultar la transacción en Wompi", transactionId, "", null);
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode data = root.path("data");
            String status = data.path("status").asText("");
            String paymentMethodType = data.path("payment_method_type").asText("CARD");

            if ("APPROVED".equalsIgnoreCase(status)) {
                // 1. Confirmar la reserva si estaba pendiente
                if (reserva.getEstado() == EstadoReserva.PENDIENTE) {
                    reserva.setEstado(EstadoReserva.CONFIRMADA);
                    reserva = reservaRepository.save(reserva);
                }

                // 2. Obtener o crear la cuenta
                Cuenta cuenta = reserva.getCuenta();
                if (cuenta == null) {
                    cuenta = Cuenta.builder()
                            .reserva(reserva)
                            .estado(EstadoCuenta.ABIERTA)
                            .fechaCreacion(LocalDateTime.now())
                            .build();
                    reserva.setCuenta(cuenta);
                    cuenta = cuentaRepository.save(cuenta);
                } else if (cuenta.getEstado() != EstadoCuenta.ABIERTA) {
                    cuenta.setEstado(EstadoCuenta.ABIERTA);
                }

                // 3. Registrar el Pago en la cuenta
                long noches = Math.max(1, ChronoUnit.DAYS.between(reserva.getFechaInicio(), reserva.getFechaFin()));
                BigDecimal montoPago = reserva.getPrecioNocheAcordado().multiply(BigDecimal.valueOf(noches));

                Pago pago = Pago.builder()
                        .cuenta(cuenta)
                        .monto(montoPago)
                        .fecha(LocalDateTime.now())
                        .metodoPago("WOMPI (" + paymentMethodType + ")")
                        .operador(null)
                        .build();

                cuenta.getPagos().add(pago);
                cuentaRepository.save(cuenta);

                return new WompiVerificarResponse(
                        true,
                        "APPROVED",
                        "Pago procesado y aprobado exitosamente con Wompi.",
                        transactionId,
                        paymentMethodType,
                        ReservaResponse.from(reserva)
                );
            } else if ("DECLINED".equalsIgnoreCase(status)) {
                return new WompiVerificarResponse(
                        false,
                        "DECLINED",
                        "La transacción fue rechazada por la entidad bancaria.",
                        transactionId,
                        paymentMethodType,
                        null
                );
            } else if ("PENDING".equalsIgnoreCase(status)) {
                return new WompiVerificarResponse(
                        false,
                        "PENDING",
                        "La transacción está pendiente de confirmación por la red bancaria (ej. PSE).",
                        transactionId,
                        paymentMethodType,
                        null
                );
            } else {
                return new WompiVerificarResponse(
                        false,
                        status,
                        "Transacción no aprobada: " + status,
                        transactionId,
                        paymentMethodType,
                        null
                );
            }
        } catch (Exception e) {
            return new WompiVerificarResponse(
                    false,
                    "ERROR",
                    "Error al comunicarse con la pasarela Wompi: " + e.getMessage(),
                    transactionId,
                    "",
                    null
            );
        }
    }
}
