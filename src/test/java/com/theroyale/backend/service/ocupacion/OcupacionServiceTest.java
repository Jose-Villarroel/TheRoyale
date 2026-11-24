package com.theroyale.backend.service.ocupacion;

import com.theroyale.backend.service.ocupacion.OcupacionService.Estancia;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// ===== Pruebas del conteo de ocupación (foto con historial y reservas reales) =====
class OcupacionServiceTest {

    private static final LocalDate NOCHE = LocalDate.of(2026, 12, 10);

    // Tipo 1 con 4 habitaciones:
    // - una estadía no cancelada que cubre la noche, hecha 30 días antes
    // - una que se canceló después, hecha 20 días antes
    // - una no cancelada hecha 2 días antes
    // - una que termina justo esa noche (salida = NOCHE): no la ocupa
    private OcupacionService.Foto foto() {
        List<Estancia> estancias = List.of(
                new Estancia(NOCHE.minusDays(30), NOCHE.minusDays(1), NOCHE.plusDays(2), false),
                new Estancia(NOCHE.minusDays(20), NOCHE, NOCHE.plusDays(1), true),
                new Estancia(NOCHE.minusDays(2), NOCHE, NOCHE.plusDays(3), false),
                new Estancia(NOCHE.minusDays(40), NOCHE.minusDays(3), NOCHE, false));
        return new OcupacionService.Foto(Map.of(1L, estancias), Map.of(1L, 4));
    }

    @Test
    void ocupacionFinal_cuentaSoloLasNoCanceladasQueCubrenLaNoche() {
        assertThat(foto().ocupacionFinal(1L, NOCHE)).isEqualTo(0.5);
    }

    @Test
    void reservada_incluyeLasQueDespuesSeCancelaron_hastaLaFechaDeCorte() {
        OcupacionService.Foto foto = foto();
        assertThat(foto.reservada(1L, NOCHE, NOCHE.minusDays(25))).isEqualTo(0.25);
        assertThat(foto.reservada(1L, NOCHE, NOCHE.minusDays(10))).isEqualTo(0.5);
        assertThat(foto.reservada(1L, NOCHE, NOCHE)).isEqualTo(0.75);
    }

    @Test
    void reservadaVigente_noCuentaCanceladas() {
        assertThat(foto().reservadaVigente(1L, NOCHE, NOCHE)).isEqualTo(0.5);
    }

    @Test
    void tipoSinHabitaciones_daCero() {
        assertThat(foto().ocupacionFinal(9L, NOCHE)).isZero();
        assertThat(foto().reservada(9L, NOCHE, NOCHE)).isZero();
    }
}
