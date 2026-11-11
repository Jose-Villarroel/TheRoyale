package com.theroyale.backend.dto.plan;

import com.theroyale.backend.model.EtiquetaPlato;
import com.theroyale.backend.model.Interes;
import com.theroyale.backend.model.Presupuesto;
import com.theroyale.backend.model.Ritmo;

import java.util.List;

// ===== Preferencias del huésped para su plan de visita (sección 6 de la especificación) =====
// intereses 1..5 sin repetidos, ritmo y presupuesto obligatorios, dieta 0..3 (VEGANO, VEGETARIANO,
// SIN_GLUTEN) y notas de hasta 300 caracteres. Las valida PlanVisitaService.
public record PreferenciasPlan(
        List<Interes> intereses,
        Ritmo ritmo,
        Presupuesto presupuesto,
        List<EtiquetaPlato> dieta,
        boolean movilidadReducida,
        boolean conNinos,
        String notas
) {
}
