package com.theroyale.backend.service.chat;

import com.theroyale.backend.errors.LimiteExcedidoException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// ===== Pruebas del límite de mensajes por IP (20 cada 5 minutos, ventana deslizante) =====
// Usa un reloj controlado para avanzar el tiempo sin esperar.
class LimiteMensajesServiceTest {

    private final RelojManual reloj = new RelojManual();
    private final LimiteMensajesService limite = new LimiteMensajesService(reloj);

    @Test
    void limiteMensajesService_registrar_mensaje21EnLaVentana_lanzaLimiteExcedido() {
        // Arrange
        for (int i = 0; i < LimiteMensajesService.MAXIMO_MENSAJES; i++) {
            limite.registrar("10.0.0.1");
        }

        // Act & Assert
        assertThatThrownBy(() -> limite.registrar("10.0.0.1"))
                .isInstanceOf(LimiteExcedidoException.class)
                .hasMessage("Too many messages, please try again in a few minutes.");
    }

    @Test
    void limiteMensajesService_registrar_otraIp_tieneSuPropioLimite() {
        // Arrange
        for (int i = 0; i < LimiteMensajesService.MAXIMO_MENSAJES; i++) {
            limite.registrar("10.0.0.1");
        }

        // Act & Assert
        assertThatCode(() -> limite.registrar("10.0.0.2")).doesNotThrowAnyException();
    }

    @Test
    void limiteMensajesService_registrar_pasadaLaVentana_vuelveAPermitir() {
        // Arrange
        for (int i = 0; i < LimiteMensajesService.MAXIMO_MENSAJES; i++) {
            limite.registrar("10.0.0.1");
        }
        reloj.avanzar(LimiteMensajesService.VENTANA.plusSeconds(1));

        // Act & Assert
        assertThatCode(() -> limite.registrar("10.0.0.1")).doesNotThrowAnyException();
    }

    private static class RelojManual extends Clock {

        private Instant ahora = Instant.parse("2026-11-11T10:00:00Z");

        void avanzar(Duration duracion) {
            ahora = ahora.plus(duracion);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }
}
