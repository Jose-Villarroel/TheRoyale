package com.theroyale.backend.dto.plan;

import com.theroyale.backend.model.TipoBloque;

// ===== Un bloque del plan, ya validado y enriquecido con los datos reales de su fuente =====
// titulo, url, traslado, bajoTecho y precio salen de la fuente; de la IA solo se usa el motivo.
// servicioId, precio y agregable solo vienen en SERVICIO_HOTEL. En TIEMPO_LIBRE el motivo es la nota.
public record BloquePlanResponse(
        String id,
        String inicio,
        String fin,
        TipoBloque tipo,
        String categoria,
        String titulo,
        String motivo,
        String traslado,
        Boolean bajoTecho,
        String lugar,
        String url,
        String fuente,
        Long servicioId,
        Double precio,
        Boolean agregable
) {

    // agregable depende del estado actual de la reserva (cuenta abierta), no del momento en que se generó
    public BloquePlanResponse conAgregable(boolean cuentaAbierta) {
        if (tipo != TipoBloque.SERVICIO_HOTEL) {
            return this;
        }
        return new BloquePlanResponse(id, inicio, fin, tipo, categoria, titulo, motivo, traslado, bajoTecho,
                lugar, url, fuente, servicioId, precio, cuentaAbierta);
    }
}
