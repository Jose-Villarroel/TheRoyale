package com.theroyale.backend.service.plan.fuentes;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

// ===== Caché en memoria con tiempo de vida (TTL), sin dependencias nuevas =====
// Cada fuente tiene la suya: 1 h para NWS y 6 h para las demás (tabla 5.2 de la especificación).
// Si la carga falla no se guarda nada, así el siguiente plan vuelve a intentar.
public class CacheTtl {

    private record Entrada(Object valor, Instant expira) {
    }

    private final Map<String, Entrada> entradas = new ConcurrentHashMap<>();
    private final Duration ttl;
    private final Clock reloj;

    public CacheTtl(Duration ttl) {
        this(ttl, Clock.systemUTC());
    }

    // Para las pruebas: permite avanzar el tiempo sin esperar
    public CacheTtl(Duration ttl, Clock reloj) {
        this.ttl = ttl;
        this.reloj = reloj;
    }

    @SuppressWarnings("unchecked")
    public <T> T obtener(String clave, Supplier<T> cargar) {
        Instant ahora = reloj.instant();
        Entrada entrada = entradas.get(clave);
        if (entrada != null && entrada.expira().isAfter(ahora)) {
            return (T) entrada.valor();
        }
        T valor = cargar.get();
        entradas.put(clave, new Entrada(valor, ahora.plus(ttl)));
        if (entradas.size() > 500) {
            entradas.values().removeIf(otra -> !otra.expira().isAfter(ahora));
        }
        return valor;
    }

    public void limpiar() {
        entradas.clear();
    }
}
