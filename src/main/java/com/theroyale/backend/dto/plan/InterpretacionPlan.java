package com.theroyale.backend.dto.plan;

import java.util.List;

// ===== Respuesta de interpretar: preferencias estructuradas y 3 a 5 frases de "What I understood" =====
public record InterpretacionPlan(
        PreferenciasPlan preferencias,
        List<EntendidoItem> entendido
) {
}
