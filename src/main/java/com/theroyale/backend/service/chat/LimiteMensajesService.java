package com.theroyale.backend.service.chat;

import com.theroyale.backend.errors.LimiteExcedidoException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// ===== Límite simple de mensajes por IP para proteger la cuota de la clave de Gemini =====
// Ventana deslizante en memoria: como máximo 20 mensajes cada 5 minutos por IP. Se pierde al
// reiniciar y no se comparte entre instancias, suficiente para un solo servidor.
@Service
public class LimiteMensajesService {

    static final int MAXIMO_MENSAJES = 20;
    static final Duration VENTANA = Duration.ofMinutes(5);
    static final String MENSAJE_LIMITE = "Too many messages, please try again in a few minutes.";

    // Por encima de este número de IPs se limpian las que ya no tienen mensajes en la ventana
    private static final int IPS_ANTES_DE_LIMPIAR = 10_000;

    private final Map<String, Deque<Instant>> mensajesPorIp = new ConcurrentHashMap<>();
    private final Clock reloj;

    public LimiteMensajesService() {
        this(Clock.systemUTC());
    }

    // Para las pruebas: permite avanzar el tiempo sin esperar
    LimiteMensajesService(Clock reloj) {
        this.reloj = reloj;
    }

    // Registra un mensaje de la IP o lanza LimiteExcedidoException (429) si ya llegó al máximo
    public void registrar(String ip) {
        Instant ahora = reloj.instant();
        Deque<Instant> marcas = mensajesPorIp.computeIfAbsent(ip == null ? "desconocida" : ip,
                clave -> new ArrayDeque<>());
        synchronized (marcas) {
            descartarVencidas(marcas, ahora);
            if (marcas.size() >= MAXIMO_MENSAJES) {
                throw new LimiteExcedidoException(MENSAJE_LIMITE);
            }
            marcas.addLast(ahora);
        }

        if (mensajesPorIp.size() > IPS_ANTES_DE_LIMPIAR) {
            mensajesPorIp.values().removeIf(otras -> {
                synchronized (otras) {
                    descartarVencidas(otras, ahora);
                    return otras.isEmpty();
                }
            });
        }
    }

    private void descartarVencidas(Deque<Instant> marcas, Instant ahora) {
        Instant limite = ahora.minus(VENTANA);
        while (!marcas.isEmpty() && !marcas.peekFirst().isAfter(limite)) {
            marcas.pollFirst();
        }
    }
}
