package com.theroyale.backend.service.precio;

import com.theroyale.backend.model.EstadoRecomendacion;
import com.theroyale.backend.model.RecomendacionPrecio;
import com.theroyale.backend.model.TarifaAplicada;
import com.theroyale.backend.model.Temporada;
import com.theroyale.backend.model.TipoHabitacion;
import com.theroyale.backend.repository.HabitacionRepository;
import com.theroyale.backend.repository.RecomendacionPrecioRepository;
import com.theroyale.backend.repository.TarifaAplicadaRepository;
import com.theroyale.backend.repository.TipoHabitacionRepository;
import com.theroyale.backend.service.ocupacion.OcupacionService;
import com.theroyale.backend.service.ocupacion.PronosticoOcupacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// ===== Pruebas de las recomendaciones de precio =====
// Reglas del cálculo (factores, límites, etiquetas) y el flujo de decisión del admin: solo aplicar
// una recomendación pendiente crea una tarifa (lo único que cambia el precio real).
@ExtendWith(MockitoExtension.class)
class RecomendacionPrecioServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 23);

    @Mock private RecomendacionPrecioRepository recomendacionRepository;
    @Mock private TarifaAplicadaRepository tarifaAplicadaRepository;
    @Mock private TipoHabitacionRepository tipoHabitacionRepository;
    @Mock private HabitacionRepository habitacionRepository;
    @Mock private TemporadaService temporadaService;
    @Mock private PrecioService precioService;
    @Mock private OcupacionService ocupacionService;
    @Mock private PronosticoOcupacionService pronosticoService;

    private RecomendacionPrecioService servicio;
    private final TipoHabitacion tipo = TipoHabitacion.builder().id(1L).nombre("Normal").build();

    @BeforeEach
    void crear() {
        servicio = new RecomendacionPrecioService(recomendacionRepository, tarifaAplicadaRepository,
                tipoHabitacionRepository, habitacionRepository, temporadaService, precioService,
                ocupacionService, pronosticoService, "-");
        lenient().when(pronosticoService.hoy()).thenReturn(HOY);
        lenient().when(recomendacionRepository.save(any(RecomendacionPrecio.class))).thenAnswer(i -> i.getArgument(0));
    }

    private RecomendacionPrecio pendiente(LocalDate lunes) {
        return RecomendacionPrecio.builder().id(7L).tipoHabitacion(tipo).semanaInicio(lunes)
                .factorRecomendado(1.43).etiqueta("High season").precioRecomendado(BigDecimal.valueOf(170))
                .estado(EstadoRecomendacion.PENDIENTE).build();
    }

    // ===== Reglas del cálculo =====
    @Test
    void factorDemanda_porTramosDeOcupacion() {
        assertThat(RecomendacionPrecioService.factorDemanda(0.30)).isEqualTo(0.90);
        assertThat(RecomendacionPrecioService.factorDemanda(0.55)).isEqualTo(1.00);
        assertThat(RecomendacionPrecioService.factorDemanda(0.80)).isEqualTo(1.10);
        assertThat(RecomendacionPrecioService.factorDemanda(0.90)).isEqualTo(1.25);
    }

    @Test
    void factorRecomendado_seLimitaEntre080Y150() {
        assertThat(RecomendacionPrecioService.factorRecomendado(1.35, 1.25)).isEqualTo(1.50);
        assertThat(RecomendacionPrecioService.factorRecomendado(0.85, 0.90)).isEqualTo(0.80);
        assertThat(RecomendacionPrecioService.factorRecomendado(1.15, 1.10)).isEqualTo(1.27);
    }

    @Test
    void etiqueta_segunQueEmpujaElPrecio() {
        assertThat(RecomendacionPrecioService.etiqueta(1.35, 1.10, 1.49)).isEqualTo("High season");
        assertThat(RecomendacionPrecioService.etiqueta(1.0, 1.25, 1.25)).isEqualTo("High demand");
        assertThat(RecomendacionPrecioService.etiqueta(0.85, 1.0, 0.85)).isEqualTo("Special rate");
    }

    @Test
    void motivo_explicaTemporadaYOcupacion() {
        Temporada fiestas = Temporada.builder().nombre("Holiday season").factor(1.35).build();
        assertThat(RecomendacionPrecioService.motivo(fiestas, 1.35, 0.914))
                .isEqualTo("Holiday season x1.35 · 91% expected occupancy");
        assertThat(RecomendacionPrecioService.motivo(null, 1.0, 0.5)).isEqualTo("No special season · 50% expected occupancy");
    }

    // ===== Decisión del admin =====
    @Test
    void aplicar_creaLaTarifaDeEsaSemanaYMarcaAplicada() {
        LocalDate lunes = HOY.plusDays(28);
        when(recomendacionRepository.findById(7L)).thenReturn(Optional.of(pendiente(lunes)));
        when(tarifaAplicadaRepository.findByTipoHabitacionId(1L)).thenReturn(List.of());

        RecomendacionPrecio r = servicio.aplicar(7L, "admin@theroyale.com");

        ArgumentCaptor<TarifaAplicada> tarifa = ArgumentCaptor.forClass(TarifaAplicada.class);
        verify(tarifaAplicadaRepository).save(tarifa.capture());
        assertThat(tarifa.getValue().getFechaInicio()).isEqualTo(lunes);
        assertThat(tarifa.getValue().getFechaFin()).isEqualTo(lunes.plusDays(6));
        assertThat(tarifa.getValue().getFactor()).isEqualTo(1.43);
        assertThat(tarifa.getValue().getAprobadaPor()).isEqualTo("admin@theroyale.com");
        assertThat(r.getEstado()).isEqualTo(EstadoRecomendacion.APLICADA);
        assertThat(r.getDecididaPor()).isEqualTo("admin@theroyale.com");
    }

    @Test
    void aplicar_unaYaDecidida_lanza409YNoCreaTarifa() {
        RecomendacionPrecio descartada = pendiente(HOY.plusDays(28));
        descartada.setEstado(EstadoRecomendacion.DESCARTADA);
        when(recomendacionRepository.findById(7L)).thenReturn(Optional.of(descartada));

        assertThatThrownBy(() -> servicio.aplicar(7L, "admin")).isInstanceOf(IllegalStateException.class);
        verify(tarifaAplicadaRepository, never()).save(any());
    }

    @Test
    void aplicar_unaSemanaQueYaEmpezo_laVenceYNoCreaTarifa() {
        RecomendacionPrecio vieja = pendiente(HOY.minusDays(1));
        when(recomendacionRepository.findById(7L)).thenReturn(Optional.of(vieja));

        assertThatThrownBy(() -> servicio.aplicar(7L, "admin")).isInstanceOf(IllegalStateException.class);
        assertThat(vieja.getEstado()).isEqualTo(EstadoRecomendacion.VENCIDA);
        verify(tarifaAplicadaRepository, never()).save(any());
    }

    @Test
    void descartar_noTocaLasTarifas() {
        when(recomendacionRepository.findById(7L)).thenReturn(Optional.of(pendiente(HOY.plusDays(28))));

        RecomendacionPrecio r = servicio.descartar(7L, "admin");

        assertThat(r.getEstado()).isEqualTo(EstadoRecomendacion.DESCARTADA);
        verify(tarifaAplicadaRepository, never()).save(any());
    }

    @Test
    void proximaGeneracion_esNullConLaTareaApagada() {
        assertThat(servicio.proximaGeneracion()).isNull();
    }
}
