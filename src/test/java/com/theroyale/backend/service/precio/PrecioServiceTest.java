package com.theroyale.backend.service.precio;

import com.theroyale.backend.model.Habitacion;
import com.theroyale.backend.model.TarifaAplicada;
import com.theroyale.backend.model.TipoHabitacion;
import com.theroyale.backend.repository.TarifaAplicadaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// ===== Pruebas del precio real por noche: base x tarifa aprobada por el admin =====
@ExtendWith(MockitoExtension.class)
class PrecioServiceTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 12, 21);

    @Mock
    private TarifaAplicadaRepository tarifaAplicadaRepository;

    @InjectMocks
    private PrecioService precioService;

    private final TipoHabitacion tipo = TipoHabitacion.builder().id(1L).nombre("Normal").build();
    private final Habitacion habitacion = Habitacion.builder().id(1L).numero("101").precio(120.0).tipoHabitacion(tipo).build();

    private TarifaAplicada tarifa(LocalDate inicio, LocalDate fin, double factor, String etiqueta, LocalDateTime aprobada) {
        return TarifaAplicada.builder().tipoHabitacion(tipo).fechaInicio(inicio).fechaFin(fin)
                .factor(factor).etiqueta(etiqueta).aprobadaPor("admin").fechaAprobacion(aprobada).build();
    }

    @Test
    void sinTarifaAprobada_cobraElPrecioBase() {
        when(tarifaAplicadaRepository.findByTipoHabitacionId(1L)).thenReturn(List.of());

        assertThat(precioService.precioPromedio(habitacion, LUNES, LUNES.plusDays(3))).isEqualByComparingTo("120.00");
        assertThat(precioService.etiqueta(1L, LUNES, LUNES.plusDays(3))).isNull();
    }

    @Test
    void conTarifaAprobada_cobraBasePorFactorRedondeadoACinco() {
        when(tarifaAplicadaRepository.findByTipoHabitacionId(1L))
                .thenReturn(List.of(tarifa(LUNES, LUNES.plusDays(6), 1.43, "High season", LocalDateTime.now())));

        // 120 x 1.43 = 171.6 -> 170
        assertThat(precioService.precioNoche(habitacion, LUNES)).isEqualByComparingTo("170.00");
        assertThat(precioService.etiqueta(1L, LUNES, LUNES.plusDays(2))).isEqualTo("High season");
    }

    @Test
    void estadiaQueCruzaElFinDeLaTarifa_promediaNochePorNoche() {
        // Tarifa x1.5 solo del lunes al domingo; la estadía va del sábado al martes siguiente
        when(tarifaAplicadaRepository.findByTipoHabitacionId(1L))
                .thenReturn(List.of(tarifa(LUNES, LUNES.plusDays(6), 1.5, "High season", LocalDateTime.now())));

        // Noches: sábado 180, domingo 180, lunes 120, martes 120 -> promedio 150
        BigDecimal promedio = precioService.precioPromedio(habitacion, LUNES.plusDays(5), LUNES.plusDays(9));
        assertThat(promedio).isEqualByComparingTo("150.00");
    }

    @Test
    void tarifasSuperpuestas_mandaLaAprobadaMasReciente() {
        LocalDateTime ayer = LocalDateTime.now().minusDays(1);
        when(tarifaAplicadaRepository.findByTipoHabitacionId(1L)).thenReturn(List.of(
                tarifa(LUNES, LUNES.plusDays(6), 1.5, "High season", ayer),
                tarifa(LUNES, LUNES.plusDays(6), 0.85, "Special rate", ayer.plusHours(5))));

        assertThat(precioService.factorVigente(1L, LUNES)).isEqualTo(0.85);
        assertThat(precioService.precioNoche(habitacion, LUNES)).isEqualByComparingTo("100.00");
    }

    @Test
    void aplicarFactor_unoDejaElBaseSinRedondear() {
        assertThat(PrecioService.aplicarFactor(123.0, 1.0)).isEqualByComparingTo("123.00");
        assertThat(PrecioService.aplicarFactor(123.0, 1.1)).isEqualByComparingTo("135.00");
    }
}
