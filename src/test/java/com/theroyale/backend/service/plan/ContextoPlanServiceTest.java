package com.theroyale.backend.service.plan;

import com.theroyale.backend.config.HotelProperties;
import com.theroyale.backend.config.PlanVisitaProperties;
import com.theroyale.backend.dto.plan.PreferenciasPlan;
import com.theroyale.backend.model.CategoriaLugar;
import com.theroyale.backend.model.EtiquetaPlato;
import com.theroyale.backend.model.FuentePlan;
import com.theroyale.backend.model.Interes;
import com.theroyale.backend.model.LugarNyc;
import com.theroyale.backend.model.PlatoMenu;
import com.theroyale.backend.model.Presupuesto;
import com.theroyale.backend.model.RestauranteHotel;
import com.theroyale.backend.model.Ritmo;
import com.theroyale.backend.model.Servicio;
import com.theroyale.backend.model.TipoClima;
import com.theroyale.backend.repository.LugarNycRepository;
import com.theroyale.backend.service.InterfaceService;
import com.theroyale.backend.service.PlatoMenuService;
import com.theroyale.backend.service.plan.ContextoPlan.EventoContexto;
import com.theroyale.backend.service.plan.fuentes.ClimaClient;
import com.theroyale.backend.service.plan.fuentes.ClimaDia;
import com.theroyale.backend.service.plan.fuentes.EventoCandidato;
import com.theroyale.backend.service.plan.fuentes.EventosClient;
import com.theroyale.backend.service.plan.fuentes.FuenteNoDisponibleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

// ===== Pruebas de ContextoPlanService: fuentes en paralelo, timeout, filtros y elección del clima =====
// Las fuentes son clientes falsos escritos aquí (con demoras controladas); repositorio y servicios del
// hotel se simulan con Mockito. "Hoy" queda fijo en el 2026-11-11 de Nueva York.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContextoPlanServiceTest {

    private static final ZoneId NUEVA_YORK = ZoneId.of("America/New_York");
    private static final Clock HOY = Clock.fixed(LocalDateTime.of(2026, 11, 11, 9, 0).atZone(NUEVA_YORK).toInstant(), NUEVA_YORK);
    private static final LocalDate LLEGADA = LocalDate.of(2026, 12, 12);

    @Mock
    private LugarNycRepository lugarNycRepository;

    @Mock
    private InterfaceService servicioService;

    @Mock
    private PlatoMenuService platoMenuService;

    private PlanVisitaProperties propiedades;

    @BeforeEach
    void setUp() {
        propiedades = new PlanVisitaProperties();
        propiedades.setTimeoutFuenteSegundos(1);
        when(lugarNycRepository.findByActivoTrueOrderByIdAsc()).thenReturn(List.of(
                lugar(1L, "The Museum of Modern Art (MoMA)", 40.7614, -73.9776, true),
                lugar(2L, "Central Park", 40.7829, -73.9654, false),
                // A unas 14 millas: queda fuera del radio de 5 millas
                lugar(3L, "JFK Airport", 40.6413, -73.7781, false)));
        when(servicioService.listarServicios()).thenReturn(List.of(Servicio.builder().id(1L).nombre("Wellness")
                .precio(45.0).caracteristicas(List.of("Signature Massage - 60 or 90 min")).build()));
    }

    // ===== Fuentes en paralelo y timeout =====
    @Test
    void contextoPlanService_construir_llamaLasFuentesEnParalelo() {
        // Arrange: cuatro fuentes que tardan 600 ms cada una
        ContextoPlanService servicio = crear(
                clima(FuentePlan.NWS, 600, (a, b) -> List.of()),
                clima(FuentePlan.OPEN_METEO, 600, (a, b) -> diasEntre(a, b, TipoClima.CLIMA_TIPICO)),
                eventos(FuentePlan.NYC_OPEN_DATA, 600, List.of()),
                eventos(FuentePlan.TICKETMASTER, 600, List.of()));
        long inicio = System.currentTimeMillis();

        // Act
        ContextoPlan contexto = servicio.construir(estadia(LLEGADA, 3), preferencias());

        // Assert: en serie tardaría 2.4 s; en paralelo, poco más de 0.6 s
        assertThat(System.currentTimeMillis() - inicio).isLessThan(1_800);
        assertThat(contexto.fuentesUsadas()).containsExactly(FuentePlan.OPEN_METEO, FuentePlan.NYC_OPEN_DATA, FuentePlan.TICKETMASTER);
        assertThat(contexto.fuentesNoDisponibles()).isEmpty();
    }

    @Test
    void contextoPlanService_construir_fuenteQueNoRespondeATiempo_quedaEnNoDisponiblesYElRestoSigue() {
        // Arrange: Ticketmaster tarda 5 s y el timeout es de 1 s
        EventoCandidato mercado = evento("nyc-1", FuentePlan.NYC_OPEN_DATA, LLEGADA.plusDays(1), 40.7359, -73.9911);
        ContextoPlanService servicio = crear(
                clima(FuentePlan.OPEN_METEO, 0, (a, b) -> diasEntre(a, b, TipoClima.CLIMA_TIPICO)),
                eventos(FuentePlan.NYC_OPEN_DATA, 0, List.of(mercado)),
                eventos(FuentePlan.TICKETMASTER, 5_000, List.of(evento("tm-1", FuentePlan.TICKETMASTER, LLEGADA, 40.765, -73.98))));
        long inicio = System.currentTimeMillis();

        // Act
        ContextoPlan contexto = servicio.construir(estadia(LLEGADA, 3), preferencias());

        // Assert
        assertThat(System.currentTimeMillis() - inicio).isLessThan(3_000);
        assertThat(contexto.fuentesNoDisponibles()).containsExactly(FuentePlan.TICKETMASTER);
        assertThat(contexto.fuentesUsadas()).contains(FuentePlan.NYC_OPEN_DATA).doesNotContain(FuentePlan.TICKETMASTER);
        assertThat(contexto.eventos()).extracting(e -> e.evento().id()).containsExactly("nyc-1");
    }

    @Test
    void contextoPlanService_construir_fuenteQueFalla_quedaEnNoDisponibles() {
        // Arrange: Ticketmaster sin clave
        ContextoPlanService servicio = crear(
                clima(FuentePlan.OPEN_METEO, 0, (a, b) -> diasEntre(a, b, TipoClima.CLIMA_TIPICO)),
                new EventosClient() {
                    public FuentePlan fuente() {
                        return FuentePlan.TICKETMASTER;
                    }

                    public List<EventoCandidato> buscar(LocalDate inicio, LocalDate fin) {
                        throw new FuenteNoDisponibleException("Ticketmaster está apagado: falta TICKETMASTER_API_KEY.");
                    }
                });

        // Act
        ContextoPlan contexto = servicio.construir(estadia(LLEGADA, 3), preferencias());

        // Assert
        assertThat(contexto.fuentesNoDisponibles()).containsExactly(FuentePlan.TICKETMASTER);
        assertThat(contexto.clima()).hasSize(4);
    }

    // ===== Filtros de distancia y fecha =====
    @Test
    void contextoPlanService_construir_filtraEventosPorFechaYDistanciaYOrdenaPorCercania() {
        // Arrange
        List<EventoCandidato> candidatos = List.of(
                evento("cerca", FuentePlan.NYC_OPEN_DATA, LLEGADA.plusDays(1), 40.7587, -73.9787),
                evento("lejos-dentro", FuentePlan.NYC_OPEN_DATA, LLEGADA.plusDays(1), 40.7033, -74.0170),
                evento("sin-coordenadas", FuentePlan.NYC_OPEN_DATA, LLEGADA.plusDays(1), null, null),
                evento("fuera-de-radio", FuentePlan.NYC_OPEN_DATA, LLEGADA.plusDays(1), 40.6413, -73.7781),
                evento("antes-de-llegar", FuentePlan.NYC_OPEN_DATA, LLEGADA.minusDays(2), 40.7587, -73.9787),
                evento("despues-de-salir", FuentePlan.NYC_OPEN_DATA, LLEGADA.plusDays(5), 40.7587, -73.9787));
        ContextoPlanService servicio = crear(
                clima(FuentePlan.OPEN_METEO, 0, (a, b) -> diasEntre(a, b, TipoClima.CLIMA_TIPICO)),
                eventos(FuentePlan.NYC_OPEN_DATA, 0, candidatos));

        // Act
        ContextoPlan contexto = servicio.construir(estadia(LLEGADA, 3), preferencias());

        // Assert
        assertThat(contexto.eventos()).extracting(e -> e.evento().id())
                .containsExactly("cerca", "lejos-dentro", "sin-coordenadas");
        assertThat(contexto.eventos().get(0).distanciaMetros()).isLessThan(200);
        assertThat(contexto.eventos().get(2).distanciaMetros()).isNull();
    }

    @Test
    void contextoPlanService_filtrarEventos_maximoQuincePorDiaYSesentaEnTotal() {
        // Arrange: 20 eventos por día durante 5 días
        List<EventoCandidato> candidatos = new ArrayList<>();
        List<LocalDate> dias = estadia(LLEGADA, 4);
        for (LocalDate dia : dias) {
            for (int i = 0; i < 20; i++) {
                candidatos.add(evento(dia + "-" + i, FuentePlan.NYC_OPEN_DATA, dia, 40.7590 + i * 0.001, -73.9772));
            }
        }
        ContextoPlanService servicio = crear();

        // Act
        List<EventoContexto> eventos = servicio.filtrarEventos(candidatos, dias);

        // Assert
        assertThat(eventos).hasSize(60);
        for (LocalDate dia : dias) {
            assertThat(eventos.stream().filter(e -> e.evento().ocurreEl(dia)).count()).isLessThanOrEqualTo(15);
        }
    }

    @Test
    void contextoPlanService_construir_lugaresConDistanciaYSoloDentroDeCincoMillas() {
        // Arrange
        ContextoPlanService servicio = crear(clima(FuentePlan.OPEN_METEO, 0, (a, b) -> diasEntre(a, b, TipoClima.CLIMA_TIPICO)));

        // Act
        ContextoPlan contexto = servicio.construir(estadia(LLEGADA, 3), preferencias());

        // Assert: MoMA queda a unos 270 m del hotel (4 min a pie)
        assertThat(contexto.lugares()).extracting(ContextoPlan.LugarContexto::nombre)
                .containsExactly("The Museum of Modern Art (MoMA)", "Central Park");
        assertThat(contexto.lugares().get(0).distanciaMetros()).isBetween(200.0, 350.0);
        assertThat(contexto.servicios()).hasSize(1);
        assertThat(contexto.restaurantes()).hasSize(4);
    }

    // ===== Clima según los días que faltan =====
    @Test
    void contextoPlanService_construir_eligeNwsOpenMeteoOClimaTipicoSegunLosDiasQueFaltan() {
        // Arrange: hoy 2026-11-11; el 13 está a 2 días, el 20 a 9 días y el 12 de diciembre a 31
        ContextoPlanService servicio = crear(
                clima(FuentePlan.NWS, 0, (a, b) -> diasEntre(a, b, TipoClima.PRONOSTICO_NWS)),
                new ClimaClient() {
                    public FuentePlan fuente() {
                        return FuentePlan.OPEN_METEO;
                    }

                    public List<ClimaDia> pronostico(LocalDate a, LocalDate b) {
                        return diasEntre(a, b, TipoClima.PRONOSTICO_OPEN_METEO);
                    }

                    public List<ClimaDia> climaTipico(LocalDate a, LocalDate b) {
                        return diasEntre(a, b, TipoClima.CLIMA_TIPICO);
                    }
                });
        LocalDate cercano = LocalDate.of(2026, 11, 13);
        LocalDate medio = LocalDate.of(2026, 11, 20);
        LocalDate lejano = LocalDate.of(2026, 12, 12);

        // Act
        ContextoPlan contexto = servicio.construir(List.of(cercano, medio, lejano), preferencias());

        // Assert
        assertThat(contexto.clima().get(cercano).tipo()).isEqualTo(TipoClima.PRONOSTICO_NWS);
        assertThat(contexto.clima().get(medio).tipo()).isEqualTo(TipoClima.PRONOSTICO_OPEN_METEO);
        assertThat(contexto.clima().get(lejano).tipo()).isEqualTo(TipoClima.CLIMA_TIPICO);
        assertThat(contexto.fuentesUsadas()).contains(FuentePlan.NWS, FuentePlan.OPEN_METEO);
    }

    @Test
    void contextoPlanService_construir_nwsCaido_usaElPronosticoDeOpenMeteoComoRespaldo() {
        // Arrange
        ContextoPlanService servicio = crear(
                clima(FuentePlan.NWS, 0, (a, b) -> {
                    throw new FuenteNoDisponibleException("NWS respondió 500");
                }),
                clima(FuentePlan.OPEN_METEO, 0, (a, b) -> diasEntre(a, b, TipoClima.PRONOSTICO_OPEN_METEO)));
        LocalDate manana = LocalDate.of(2026, 11, 12);

        // Act
        ContextoPlan contexto = servicio.construir(List.of(manana), preferencias());

        // Assert
        assertThat(contexto.clima().get(manana).tipo()).isEqualTo(TipoClima.PRONOSTICO_OPEN_METEO);
        assertThat(contexto.fuentesNoDisponibles()).containsExactly(FuentePlan.NWS);
        assertThat(contexto.fuentesUsadas()).containsExactly(FuentePlan.OPEN_METEO);
    }

    @Test
    void contextoPlanService_construir_openMeteoCaido_usaLasNormalesDeCentralPark() {
        // Arrange
        ContextoPlanService servicio = crear(clima(FuentePlan.OPEN_METEO, 0, (a, b) -> {
            throw new FuenteNoDisponibleException("Too many concurrent requests");
        }));

        // Act
        ContextoPlan contexto = servicio.construir(estadia(LLEGADA, 3), preferencias());

        // Assert: el día sigue teniendo clima típico y la fuente aparece como no disponible
        assertThat(contexto.clima().values()).allMatch(c -> c.tipo() == TipoClima.CLIMA_TIPICO && c.maxC() == 7);
        assertThat(contexto.fuentesNoDisponibles()).containsExactly(FuentePlan.OPEN_METEO);
    }

    @Test
    void contextoPlanService_construir_conDieta_cuentaPlatosPorRestaurante() {
        // Arrange
        when(platoMenuService.listar(any(), eq(null), eq(EtiquetaPlato.VEGANO), eq(true))).thenReturn(List.of());
        when(platoMenuService.listar(eq(RestauranteHotel.SIGNATURE_RESTAURANT), eq(null), eq(EtiquetaPlato.VEGANO), eq(true)))
                .thenReturn(List.of(new PlatoMenu(), new PlatoMenu()));
        ContextoPlanService servicio = crear(clima(FuentePlan.OPEN_METEO, 0, (a, b) -> diasEntre(a, b, TipoClima.CLIMA_TIPICO)));
        PreferenciasPlan veganos = new PreferenciasPlan(List.of(Interes.GASTRONOMIA), Ritmo.EQUILIBRADO,
                Presupuesto.ALTO, List.of(EtiquetaPlato.VEGANO), false, false, null);

        // Act
        ContextoPlan contexto = servicio.construir(estadia(LLEGADA, 2), veganos);

        // Assert
        assertThat(contexto.restaurante("SIGNATURE_RESTAURANT").orElseThrow().opcionesDieta())
                .containsExactly("VEGANO: 2 dishes");
    }

    // ===== Ayudas =====
    private ContextoPlanService crear(Object... fuentes) {
        List<ClimaClient> climas = new ArrayList<>();
        List<EventosClient> eventos = new ArrayList<>();
        for (Object fuente : fuentes) {
            if (fuente instanceof ClimaClient c) {
                climas.add(c);
            } else {
                eventos.add((EventosClient) fuente);
            }
        }
        return new ContextoPlanService(climas, eventos, lugarNycRepository, servicioService, platoMenuService,
                new HotelProperties(), propiedades, HOY);
    }

    private static ClimaClient clima(FuentePlan fuente, long demoraMs,
                                     BiFunction<LocalDate, LocalDate, List<ClimaDia>> datos) {
        return new ClimaClient() {
            public FuentePlan fuente() {
                return fuente;
            }

            public List<ClimaDia> pronostico(LocalDate inicio, LocalDate fin) {
                esperar(demoraMs);
                return datos.apply(inicio, fin);
            }

            public List<ClimaDia> climaTipico(LocalDate inicio, LocalDate fin) {
                esperar(demoraMs);
                return datos.apply(inicio, fin);
            }
        };
    }

    private static EventosClient eventos(FuentePlan fuente, long demoraMs, List<EventoCandidato> datos) {
        return new EventosClient() {
            public FuentePlan fuente() {
                return fuente;
            }

            public List<EventoCandidato> buscar(LocalDate inicio, LocalDate fin) {
                esperar(demoraMs);
                return datos;
            }
        };
    }

    private static void esperar(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    static List<ClimaDia> diasEntre(LocalDate inicio, LocalDate fin, TipoClima tipo) {
        List<ClimaDia> dias = new ArrayList<>();
        for (LocalDate d = inicio; !d.isAfter(fin); d = d.plusDays(1)) {
            dias.add(new ClimaDia(d, "Cloudy", 8, 2, 20, tipo));
        }
        return dias;
    }

    static List<LocalDate> estadia(LocalDate llegada, int noches) {
        List<LocalDate> dias = new ArrayList<>();
        for (int i = 0; i <= noches; i++) {
            dias.add(llegada.plusDays(i));
        }
        return dias;
    }

    private static EventoCandidato evento(String id, FuentePlan fuente, LocalDate dia, Double latitud, Double longitud) {
        return new EventoCandidato(id, fuente, "Evento " + id, dia.atTime(10, 0), dia.atTime(18, 0), "Lugar",
                latitud, longitud, "FERIA", "https://example.org/" + id, false);
    }

    private static LugarNyc lugar(Long id, String nombre, double latitud, double longitud, boolean bajoTecho) {
        return LugarNyc.builder().id(id).nombre(nombre).categoria(CategoriaLugar.MUSEO).latitud(latitud)
                .longitud(longitud).bajoTecho(bajoTecho).duracionMinutos(90).horarioReferencia("Daily")
                .urlOficial("https://example.org").activo(true).build();
    }

    private static PreferenciasPlan preferencias() {
        return new PreferenciasPlan(List.of(Interes.MUSEOS), Ritmo.EQUILIBRADO, Presupuesto.ALTO, List.of(), false, false, null);
    }
}
