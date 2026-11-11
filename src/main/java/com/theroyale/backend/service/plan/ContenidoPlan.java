package com.theroyale.backend.service.plan;

import com.theroyale.backend.dto.plan.DiaPlanResponse;
import com.theroyale.backend.dto.plan.EntendidoItem;

import java.time.LocalDate;
import java.util.List;

// ===== Lo que se guarda en PlanVisita.contenido (JSON): el plan ya validado y enriquecido =====
public record ContenidoPlan(
        String resumen,
        List<EntendidoItem> entendido,
        List<DiaPlanResponse> dias,
        List<LocalDate> diasEnRevision
) {
}
