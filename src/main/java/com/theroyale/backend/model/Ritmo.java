package com.theroyale.backend.model;

// ===== Ritmo del plan de visita: cuántos bloques de actividad se sugieren por día =====
public enum Ritmo {
    TRANQUILO(2), EQUILIBRADO(3), INTENSO(4);

    private final int bloquesPorDia;

    Ritmo(int bloquesPorDia) {
        this.bloquesPorDia = bloquesPorDia;
    }

    public int getBloquesPorDia() {
        return bloquesPorDia;
    }
}
