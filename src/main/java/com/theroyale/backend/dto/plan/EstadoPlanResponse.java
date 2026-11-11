package com.theroyale.backend.dto.plan;

// ===== GET /api/plan/estado: false si no hay GEMINI_API_KEY ("The planner is unavailable right now") =====
public record EstadoPlanResponse(
        boolean disponible
) {
}
