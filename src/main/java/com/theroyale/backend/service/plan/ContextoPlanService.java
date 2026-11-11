package com.theroyale.backend.service.plan;

import com.theroyale.backend.config.HotelProperties;
import com.theroyale.backend.config.PlanVisitaProperties;
import com.theroyale.backend.dto.plan.PreferenciasPlan;
import com.theroyale.backend.model.EtiquetaPlato;
import com.theroyale.backend.model.FuentePlan;
import com.theroyale.backend.model.LugarNyc;
import com.theroyale.backend.model.RestauranteHotel;
import com.theroyale.backend.model.TipoClima;
import com.theroyale.backend.repository.LugarNycRepository;
import com.theroyale.backend.service.InterfaceService;
import com.theroyale.backend.service.PlatoMenuService;
import com.theroyale.backend.service.plan.ContextoPlan.EventoContexto;
import com.theroyale.backend.service.plan.ContextoPlan.LugarContexto;
import com.theroyale.backend.service.plan.ContextoPlan.RestauranteContexto;
import com.theroyale.backend.service.plan.ContextoPlan.ServicioContexto;
import com.theroyale.backend.service.plan.fuentes.ClimaClient;
import com.theroyale.backend.service.plan.fuentes.ClimaDia;
import com.theroyale.backend.service.plan.fuentes.DescripcionClima;
import com.theroyale.backend.service.plan.fuentes.Distancias;
import com.theroyale.backend.service.plan.fuentes.EventoCandidato;
import com.theroyale.backend.service.plan.fuentes.EventosClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;

// ===== Arma el contexto de una generación: clima, eventos, lugares, servicios y restaurantes =====
// Las fuentes externas se llaman EN PARALELO con CompletableFuture y un timeout de 5 s cada una; si
// una falla o tarda, el plan sigue sin ella y la fuente queda en fuentesNoDisponibles.
// Clima por día según cuántos días faltan: 1-7 NWS, 8-16 pronóstico de Open-Meteo, más lejos clima
// típico de Open-Meteo. Si un día se queda sin clima se usa la normal mensual de Central Park.
// Filtros de la sección 5.2: eventos dentro de la estadía, a menos de 5 millas, hasta 15 por día y
// 60 en total, ordenados por cercanía; lugares con su distancia a pie desde el hotel.
@Service
public class ContextoPlanService {

    static final ZoneId ZONA_NUEVA_YORK = ZoneId.of("America/New_York");
    static final int DIAS_NWS = 7;
    static final int DIAS_PRONOSTICO_OPEN_METEO = 16;
    static final int MAXIMO_EVENTOS_POR_DIA = 15;
    static final int MAXIMO_EVENTOS_TOTAL = 60;

    private static final List<EtiquetaPlato> DIETAS_PERMITIDAS =
            List.of(EtiquetaPlato.VEGANO, EtiquetaPlato.VEGETARIANO, EtiquetaPlato.SIN_GLUTEN);

    private static final Logger log = LoggerFactory.getLogger(ContextoPlanService.class);

    private final Map<FuentePlan, ClimaClient> climaClients = new EnumMap<>(FuentePlan.class);
    private final List<EventosClient> eventosClients;
    private final LugarNycRepository lugarNycRepository;
    private final InterfaceService servicioService;
    private final PlatoMenuService platoMenuService;
    private final HotelProperties hotel;
    private final PlanVisitaProperties propiedades;
    private final Clock reloj;

    // Hilos daemon: no impiden que la aplicación se detenga
    private final ExecutorService ejecutor = Executors.newCachedThreadPool(tarea -> {
        Thread hilo = new Thread(tarea, "fuentes-plan");
        hilo.setDaemon(true);
        return hilo;
    });

    @Autowired
    public ContextoPlanService(List<ClimaClient> climaClients, List<EventosClient> eventosClients,
                               LugarNycRepository lugarNycRepository, InterfaceService servicioService,
                               PlatoMenuService platoMenuService, HotelProperties hotel,
                               PlanVisitaProperties propiedades) {
        this(climaClients, eventosClients, lugarNycRepository, servicioService, platoMenuService, hotel,
                propiedades, Clock.system(ZONA_NUEVA_YORK));
    }

    // Para las pruebas: fija el "hoy" que decide qué fuente de clima corresponde a cada día
    ContextoPlanService(List<ClimaClient> climaClients, List<EventosClient> eventosClients,
                        LugarNycRepository lugarNycRepository, InterfaceService servicioService,
                        PlatoMenuService platoMenuService, HotelProperties hotel,
                        PlanVisitaProperties propiedades, Clock reloj) {
        climaClients.forEach(cliente -> this.climaClients.put(cliente.fuente(), cliente));
        this.eventosClients = eventosClients;
        this.lugarNycRepository = lugarNycRepository;
        this.servicioService = servicioService;
        this.platoMenuService = platoMenuService;
        this.hotel = hotel;
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    // ===== Contexto completo de una generación (llama a las fuentes externas) =====
    public ContextoPlan construir(List<LocalDate> dias, PreferenciasPlan preferencias) {
        LocalDate inicio = dias.get(0);
        LocalDate fin = dias.get(dias.size() - 1);
        LocalDate hoy = LocalDate.now(reloj);

        List<LocalDate> diasNws = dias.stream().filter(d -> diasDesdeHoy(hoy, d) < DIAS_NWS).toList();
        List<LocalDate> diasPronostico = dias.stream().filter(d -> diasDesdeHoy(hoy, d) < DIAS_PRONOSTICO_OPEN_METEO).toList();
        List<LocalDate> diasTipicos = dias.stream().filter(d -> diasDesdeHoy(hoy, d) >= DIAS_PRONOSTICO_OPEN_METEO).toList();

        // ===== Todas las consultas salen a la vez =====
        CompletableFuture<List<ClimaDia>> nws = diasNws.isEmpty() ? null
                : llamar(FuentePlan.NWS, c -> c.pronostico(diasNws.get(0), diasNws.get(diasNws.size() - 1)));
        CompletableFuture<List<ClimaDia>> pronostico = diasPronostico.isEmpty() ? null
                : llamar(FuentePlan.OPEN_METEO, c -> c.pronostico(diasPronostico.get(0), diasPronostico.get(diasPronostico.size() - 1)));
        CompletableFuture<List<ClimaDia>> tipico = diasTipicos.isEmpty() ? null
                : llamar(FuentePlan.OPEN_METEO, c -> c.climaTipico(diasTipicos.get(0), diasTipicos.get(diasTipicos.size() - 1)));
        Map<FuentePlan, CompletableFuture<List<EventoCandidato>>> eventosPorFuente = new LinkedHashMap<>();
        for (EventosClient cliente : eventosClients) {
            eventosPorFuente.put(cliente.fuente(), conTimeout(() -> cliente.buscar(inicio, fin)));
        }

        // ===== Mientras tanto, los datos locales =====
        List<LugarContexto> lugares = lugares();
        List<ServicioContexto> servicios = servicios();
        List<RestauranteContexto> restaurantes = restaurantes(preferencias);

        Set<FuentePlan> usadas = new LinkedHashSet<>();
        Set<FuentePlan> fallidas = EnumSet.noneOf(FuentePlan.class);

        // ===== Clima por día: fuente principal según los días que faltan, luego respaldos =====
        Map<LocalDate, ClimaDia> deNws = porFecha(esperar(FuentePlan.NWS, nws, fallidas));
        Map<LocalDate, ClimaDia> dePronostico = porFecha(esperar(FuentePlan.OPEN_METEO, pronostico, fallidas));
        Map<LocalDate, ClimaDia> deTipico = porFecha(esperar(FuentePlan.OPEN_METEO, tipico, fallidas));

        Set<FuentePlan> faltantes = EnumSet.noneOf(FuentePlan.class);
        Map<LocalDate, ClimaDia> clima = new LinkedHashMap<>();
        for (LocalDate dia : dias) {
            long faltan = diasDesdeHoy(hoy, dia);
            ClimaDia elegido;
            if (faltan < DIAS_NWS) {
                elegido = Optional.ofNullable(deNws.get(dia)).orElse(dePronostico.get(dia));
                if (deNws.get(dia) == null && fallidas.contains(FuentePlan.NWS)) {
                    faltantes.add(FuentePlan.NWS);
                }
            } else if (faltan < DIAS_PRONOSTICO_OPEN_METEO) {
                elegido = dePronostico.get(dia);
            } else {
                elegido = deTipico.get(dia);
            }
            if (elegido == null && faltan >= DIAS_NWS && fallidas.contains(FuentePlan.OPEN_METEO)) {
                faltantes.add(FuentePlan.OPEN_METEO);
            }
            if (elegido == null) {
                elegido = DescripcionClima.normalDelMes(dia);
            } else {
                usadas.add(elegido.tipo() == TipoClima.PRONOSTICO_NWS
                        ? FuentePlan.NWS : FuentePlan.OPEN_METEO);
            }
            clima.put(dia, elegido);
        }

        // ===== Eventos de todas las fuentes, filtrados =====
        List<EventoCandidato> candidatos = new ArrayList<>();
        eventosPorFuente.forEach((fuente, futuro) -> {
            List<EventoCandidato> eventos = esperar(fuente, futuro, faltantes);
            if (eventos != null) {
                usadas.add(fuente);
                candidatos.addAll(eventos);
            }
        });

        List<FuentePlan> noDisponibles = new ArrayList<>(faltantes);
        Collections.sort(noDisponibles);
        usadas.removeAll(faltantes);
        List<FuentePlan> usadasOrdenadas = new ArrayList<>(usadas);
        Collections.sort(usadasOrdenadas);

        return new ContextoPlan(List.copyOf(dias), clima, lugares, filtrarEventos(candidatos, dias),
                servicios, restaurantes, usadasOrdenadas, noDisponibles);
    }

    // ===== Contexto sin fuentes externas (plan de ejemplo del DataLoader): clima típico de las normales =====
    public ContextoPlan construirSinFuentes(List<LocalDate> dias, PreferenciasPlan preferencias) {
        Map<LocalDate, ClimaDia> clima = new LinkedHashMap<>();
        dias.forEach(dia -> clima.put(dia, DescripcionClima.normalDelMes(dia)));
        return new ContextoPlan(List.copyOf(dias), clima, lugares(), List.of(), servicios(),
                restaurantes(preferencias), List.of(), List.of());
    }

    // ===== Tarea diaria de clima: pronóstico de NWS de esos días (lanza excepción si NWS falla) =====
    public Map<LocalDate, ClimaDia> pronosticoNws(LocalDate inicio, LocalDate fin) {
        ClimaClient nws = climaClients.get(FuentePlan.NWS);
        if (nws == null) {
            return Map.of();
        }
        return porFecha(conTimeout(() -> nws.pronostico(inicio, fin)).join());
    }

    // ===== Filtros de eventos: dentro de la estadía, a menos de 5 millas, 15 por día y 60 en total =====
    List<EventoContexto> filtrarEventos(List<EventoCandidato> candidatos, List<LocalDate> dias) {
        Map<String, EventoContexto> unicos = new LinkedHashMap<>();
        for (EventoCandidato evento : candidatos) {
            if (unicos.containsKey(evento.id()) || dias.stream().noneMatch(evento::ocurreEl)) {
                continue;
            }
            Double distancia = evento.tieneCoordenadas()
                    ? Distancias.metros(hotel.getLatitud(), hotel.getLongitud(), evento.latitud(), evento.longitud())
                    : null;
            if (distancia != null && distancia > Distancias.CINCO_MILLAS_METROS) {
                continue;
            }
            unicos.put(evento.id(), new EventoContexto(evento, distancia));
        }

        // Los más cercanos primero; los que no tienen coordenadas, al final
        Comparator<EventoContexto> porCercania = Comparator.comparing(
                (EventoContexto e) -> e.distanciaMetros() == null ? Double.MAX_VALUE : e.distanciaMetros())
                .thenComparing(e -> e.evento().inicio());
        List<EventoContexto> ordenados = unicos.values().stream().sorted(porCercania).toList();

        Set<String> elegidos = new LinkedHashSet<>();
        for (LocalDate dia : dias) {
            ordenados.stream()
                    .filter(e -> e.evento().ocurreEl(dia))
                    .limit(MAXIMO_EVENTOS_POR_DIA)
                    .forEach(e -> elegidos.add(e.evento().id()));
        }
        return ordenados.stream()
                .filter(e -> elegidos.contains(e.evento().id()))
                .limit(MAXIMO_EVENTOS_TOTAL)
                .toList();
    }

    // ===== Datos locales =====
    private List<LugarContexto> lugares() {
        List<LugarContexto> lugares = new ArrayList<>();
        for (LugarNyc lugar : lugarNycRepository.findByActivoTrueOrderByIdAsc()) {
            double distancia = Distancias.metros(hotel.getLatitud(), hotel.getLongitud(), lugar.getLatitud(), lugar.getLongitud());
            if (distancia <= Distancias.CINCO_MILLAS_METROS) {
                lugares.add(new LugarContexto(lugar.getId(), lugar.getNombre(), lugar.getCategoria(), lugar.isBajoTecho(),
                        lugar.getDuracionMinutos(), lugar.getHorarioReferencia(), lugar.getUrlOficial(), distancia));
            }
        }
        return lugares;
    }

    private List<ServicioContexto> servicios() {
        return servicioService.listarServicios().stream()
                .map(s -> new ServicioContexto(s.getId(), s.getNombre(), s.getPrecio(), List.copyOf(s.getCaracteristicas())))
                .toList();
    }

    private List<RestauranteContexto> restaurantes(PreferenciasPlan preferencias) {
        List<EtiquetaPlato> dietas = preferencias == null || preferencias.dieta() == null ? List.of()
                : preferencias.dieta().stream().filter(DIETAS_PERMITIDAS::contains).distinct().toList();
        List<RestauranteContexto> restaurantes = new ArrayList<>();
        for (RestauranteHotel restaurante : RestauranteHotel.values()) {
            List<String> opciones = new ArrayList<>();
            for (EtiquetaPlato dieta : dietas) {
                int platos = platoMenuService.listar(restaurante, null, dieta, true).size();
                opciones.add(dieta.name() + ": " + platos + " dishes");
            }
            restaurantes.add(new RestauranteContexto(restaurante, HorariosRestaurantes.nombre(restaurante),
                    HorariosRestaurantes.horario(restaurante), opciones));
        }
        return restaurantes;
    }

    // ===== Llamadas en paralelo =====
    private CompletableFuture<List<ClimaDia>> llamar(FuentePlan fuente,
                                                     Function<ClimaClient, List<ClimaDia>> consulta) {
        ClimaClient cliente = climaClients.get(fuente);
        if (cliente == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Sin cliente de clima " + fuente));
        }
        return conTimeout(() -> consulta.apply(cliente));
    }

    private <T> CompletableFuture<T> conTimeout(Supplier<T> consulta) {
        return CompletableFuture.supplyAsync(consulta, ejecutor)
                .orTimeout(propiedades.getTimeoutFuenteSegundos(), TimeUnit.SECONDS);
    }

    // Devuelve null si la fuente falló o tardó más del timeout, y la anota en fallidas
    private <T> T esperar(FuentePlan fuente, CompletableFuture<T> futuro, Set<FuentePlan> fallidas) {
        if (futuro == null) {
            return null;
        }
        try {
            return futuro.join();
        } catch (CompletionException ex) {
            Throwable causa = ex.getCause() == null ? ex : ex.getCause();
            String motivo = causa instanceof TimeoutException ? "no respondió a tiempo" : causa.getMessage();
            log.warn("Fuente {} no disponible para el plan: {}", fuente, motivo);
            fallidas.add(fuente);
            return null;
        }
    }

    private static Map<LocalDate, ClimaDia> porFecha(List<ClimaDia> dias) {
        Map<LocalDate, ClimaDia> mapa = new LinkedHashMap<>();
        if (dias != null) {
            dias.forEach(dia -> mapa.putIfAbsent(dia.fecha(), dia));
        }
        return mapa;
    }

    private static long diasDesdeHoy(LocalDate hoy, LocalDate dia) {
        return ChronoUnit.DAYS.between(hoy, dia);
    }
}
