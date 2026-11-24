package com.theroyale.backend.service.ocupacion;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

// ===== Fechas de alta demanda en Nueva York =====
// Las mismas que usa Docs/modelo/generar_historico.py (función feriados): Thanksgiving +-2 días,
// 20 de diciembre a 1 de enero y 3 a 5 de julio. Si cambian allá, hay que cambiarlas aquí.
public final class FeriadosNyc {

    private FeriadosNyc() {
    }

    public static boolean esAltaDemanda(LocalDate fecha) {
        int mes = fecha.getMonthValue();
        int dia = fecha.getDayOfMonth();
        if ((mes == 12 && dia >= 20) || (mes == 1 && dia == 1)) {
            return true;
        }
        if (mes == 7 && dia >= 3 && dia <= 5) {
            return true;
        }
        LocalDate thanksgiving = LocalDate.of(fecha.getYear(), 11, 1)
                .with(TemporalAdjusters.dayOfWeekInMonth(4, DayOfWeek.THURSDAY));
        return !fecha.isBefore(thanksgiving.minusDays(2)) && !fecha.isAfter(thanksgiving.plusDays(2));
    }
}
