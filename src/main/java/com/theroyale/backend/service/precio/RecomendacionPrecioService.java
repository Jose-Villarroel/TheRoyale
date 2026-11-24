package com.theroyale.backend.service.precio;

import com.theroyale.backend.errors.RecursoNoEncontradoException;
import com.theroyale.backend.model.EstadoRecomendacion;
import com.theroyale.backend.model.Habitacion;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

// ===== Recomendaciones semanales de precio (funcionalidad 10) =====
// Para cada tipo de habitación y cada una de las próximas 8 semanas propone un factor:
//   temporada (la que definió el admin) x demanda (ocupación esperada según el pronóstico),
//   limitado entre 0.80 y 1.50. El precio real solo cambia cuando el admin aplica la recomendación.
// Se genera los lunes a las 06:00 (hora de NY), a pedido, y al abrir el panel si la última tiene
// más de 7 días (por si el servidor estuvo dormido el lunes).
@Service
@Transactional(readOnly = true)
public class RecomendacionPrecioService {

    private static final Logger log = LoggerFactory.getLogger(RecomendacionPrecioService.class);

    public static final int SEMANAS = 8;
    public static final double FACTOR_MINIMO = 0.80;
    public static final double FACTOR_MAXIMO = 1.50;
    // Diferencias menores a 2.5 puntos con el precio vigente no merecen una recomendación
    private static final double DIFERENCIA_MINIMA = 0.025;

    private final RecomendacionPrecioRepository recomendacionRepository;
    private final TarifaAplicadaRepository tarifaAplicadaRepository;
    private final TipoHabitacionRepository tipoHabitacionRepository;
    private final HabitacionRepository habitacionRepository;
    private final TemporadaService temporadaService;
    private final PrecioService precioService;
    private final OcupacionService ocupacionService;
    private final PronosticoOcupacionService pronosticoService;
    private final String cron;

    public RecomendacionPrecioService(RecomendacionPrecioRepository recomendacionRepository,
                                      TarifaAplicadaRepository tarifaAplicadaRepository,
                                      TipoHabitacionRepository tipoHabitacionRepository,
                                      HabitacionRepository habitacionRepository, TemporadaService temporadaService,
                                      PrecioService precioService, OcupacionService ocupacionService,
                                      PronosticoOcupacionService pronosticoService,
                                      @Value("${precio.recomendaciones.cron:0 0 6 * * MON}") String cron) {
        this.recomendacionRepository = recomendacionRepository;
        this.tarifaAplicadaRepository = tarifaAplicadaRepository;
        this.tipoHabitacionRepository = tipoHabitacionRepository;
        this.habitacionRepository = habitacionRepository;
        this.temporadaService = temporadaService;
        this.precioService = precioService;
        this.ocupacionService = ocupacionService;
        this.pronosticoService = pronosticoService;
        this.cron = cron;
    }

    // ===== Factor por demanda según la ocupación esperada =====
    public static double factorDemanda(double ocupacion) {
        if (ocupacion < 0.40) {
            return 0.90;
        }
        if (ocupacion < 0.70) {
            return 1.00;
        }
        if (ocupacion < 0.85) {
            return 1.10;
        }
        return 1.25;
    }

    public static double factorRecomendado(double factorTemporada, double factorDemanda) {
        // BigDecimal para redondear exacto: 1.15 x 1.10 = 1.265 debe dar 1.27 (con double daba 1.26)
        double factor = BigDecimal.valueOf(factorTemporada).multiply(BigDecimal.valueOf(factorDemanda))
                .setScale(2, java.math.RoundingMode.HALF_UP).doubleValue();
        return Math.max(FACTOR_MINIMO, Math.min(FACTOR_MAXIMO, factor));
    }

    // ===== Tarea semanal =====
    @Scheduled(cron = "${precio.recomendaciones.cron:0 0 6 * * MON}", zone = "America/New_York")
    @Transactional
    public void generarProgramado() {
        int creadas = generar().size();
        log.info("Recomendaciones de precio generadas: {}", creadas);
    }

    // ===== Genera solo si nunca se generó o la última tiene más de 7 días =====
    @Transactional
    public void generarSiCorresponde() {
        Optional<LocalDateTime> ultima = recomendacionRepository.ultimaGeneracion();
        if (ultima.isEmpty() || ultima.get().isBefore(ahora().minusDays(7))) {
            generar();
        }
    }

    // ===== Recalcula las próximas 8 semanas para todos los tipos =====
    // Actualiza las pendientes de la misma semana en vez de duplicarlas, y borra las pendientes que
    // dejaron de tener sentido (el factor ya coincide con el vigente).
    @Transactional
    public List<RecomendacionPrecio> generar() {
        LocalDate hoy = pronosticoService.hoy();
        vencerPendientes(hoy);
        LocalDate primerLunes = hoy.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        List<Temporada> temporadas = temporadaService.listar();
        OcupacionService.Foto foto = ocupacionService.foto();
        LocalDateTime generada = ahora();

        List<RecomendacionPrecio> resultado = new ArrayList<>();
        for (TipoHabitacion tipo : tipoHabitacionRepository.findAll()) {
            List<Habitacion> habitaciones = habitacionRepository.findByTipoHabitacionIdOrderByNumeroAsc(tipo.getId());
            if (habitaciones.isEmpty()) {
                continue;
            }
            double base = habitaciones.stream().mapToDouble(Habitacion::getPrecio).min().orElse(0);
            for (int s = 0; s < SEMANAS; s++) {
                LocalDate lunes = primerLunes.plusWeeks(s);
                double sumaTemporada = 0, sumaOcupacion = 0, sumaActual = 0;
                Temporada temporadaPrincipal = null;
                for (int d = 0; d < 7; d++) {
                    LocalDate noche = lunes.plusDays(d);
                    Optional<Temporada> temporada = temporadaService.temporadaDe(noche, temporadas);
                    sumaTemporada += temporada.map(Temporada::getFactor).orElse(1.0);
                    if (temporadaPrincipal == null && temporada.isPresent()) {
                        temporadaPrincipal = temporada.get();
                    }
                    sumaOcupacion += pronosticoService.ocupacionEsperada(foto, tipo, noche, hoy);
                    sumaActual += precioService.factorVigente(tipo.getId(), noche);
                }
                double fTemporada = Math.round(sumaTemporada / 7 * 100) / 100.0;
                double ocupacion = sumaOcupacion / 7;
                double fDemanda = factorDemanda(ocupacion);
                double fRecomendado = factorRecomendado(fTemporada, fDemanda);
                double fActual = Math.round(sumaActual / 7 * 100) / 100.0;

                Optional<RecomendacionPrecio> pendiente = recomendacionRepository
                        .findFirstByTipoHabitacionIdAndSemanaInicioAndEstado(tipo.getId(), lunes, EstadoRecomendacion.PENDIENTE);
                if (Math.abs(fRecomendado - fActual) < DIFERENCIA_MINIMA) {
                    pendiente.ifPresent(recomendacionRepository::delete);
                    continue;
                }
                RecomendacionPrecio r = pendiente.orElseGet(RecomendacionPrecio::new);
                r.setTipoHabitacion(tipo);
                r.setSemanaInicio(lunes);
                r.setFactorTemporada(fTemporada);
                r.setOcupacionEsperada(Math.round(ocupacion * 1000) / 1000.0);
                r.setFactorDemanda(fDemanda);
                r.setFactorRecomendado(fRecomendado);
                r.setFactorActual(fActual);
                r.setPrecioBaseReferencia(BigDecimal.valueOf(base).setScale(2));
                r.setPrecioActual(PrecioService.aplicarFactor(base, fActual));
                r.setPrecioRecomendado(PrecioService.aplicarFactor(base, fRecomendado));
                r.setMotivo(motivo(temporadaPrincipal, fTemporada, ocupacion));
                r.setEtiqueta(etiqueta(fTemporada, fDemanda, fRecomendado));
                r.setEstado(EstadoRecomendacion.PENDIENTE);
                r.setFechaGeneracion(generada);
                resultado.add(recomendacionRepository.save(r));
            }
        }
        return resultado;
    }

    public List<RecomendacionPrecio> listar(EstadoRecomendacion estado) {
        return estado == null
                ? recomendacionRepository.findAllByOrderBySemanaInicioAscTipoHabitacionIdAsc()
                : recomendacionRepository.findByEstadoOrderBySemanaInicioAscTipoHabitacionIdAsc(estado);
    }

    // ===== El admin aplica una recomendación: crea la tarifa que cambia el precio real =====
    @Transactional
    public RecomendacionPrecio aplicar(Long id, String quien) {
        RecomendacionPrecio r = obtenerPendiente(id);
        // Si ya había una tarifa para exactamente esa semana y tipo, la nueva la reemplaza
        tarifaAplicadaRepository.findByTipoHabitacionId(r.getTipoHabitacion().getId()).stream()
                .filter(t -> t.getFechaInicio().equals(r.getSemanaInicio()) && t.getFechaFin().equals(r.getSemanaFin()))
                .forEach(tarifaAplicadaRepository::delete);
        tarifaAplicadaRepository.save(TarifaAplicada.builder()
                .tipoHabitacion(r.getTipoHabitacion())
                .fechaInicio(r.getSemanaInicio())
                .fechaFin(r.getSemanaFin())
                .factor(r.getFactorRecomendado())
                .etiqueta(r.getEtiqueta())
                .aprobadaPor(quien == null ? "admin" : quien)
                .fechaAprobacion(ahora())
                .recomendacionId(r.getId())
                .build());
        return decidir(r, EstadoRecomendacion.APLICADA, quien);
    }

    // ===== Aplica todas las pendientes de un tipo de habitación =====
    @Transactional
    public List<RecomendacionPrecio> aplicarTipo(Long tipoHabitacionId, String quien) {
        List<RecomendacionPrecio> aplicadas = new ArrayList<>();
        for (RecomendacionPrecio r : recomendacionRepository
                .findByEstadoOrderBySemanaInicioAscTipoHabitacionIdAsc(EstadoRecomendacion.PENDIENTE)) {
            if (r.getTipoHabitacion().getId().equals(tipoHabitacionId) && r.getSemanaInicio().isAfter(pronosticoService.hoy())) {
                aplicadas.add(aplicar(r.getId(), quien));
            }
        }
        return aplicadas;
    }

    @Transactional
    public RecomendacionPrecio descartar(Long id, String quien) {
        return decidir(obtenerPendiente(id), EstadoRecomendacion.DESCARTADA, quien);
    }

    // ===== Tarifas aprobadas =====
    public List<TarifaAplicada> tarifas() {
        return tarifaAplicadaRepository.findAllByOrderByFechaInicioAscTipoHabitacionIdAsc();
    }

    // Revertir una tarifa: esas noches vuelven al precio base (las reservas hechas no cambian)
    @Transactional
    public void revertirTarifa(Long id) {
        TarifaAplicada tarifa = tarifaAplicadaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Applied rate not found: " + id));
        tarifaAplicadaRepository.delete(tarifa);
    }

    public Optional<LocalDateTime> ultimaGeneracion() {
        return recomendacionRepository.ultimaGeneracion();
    }

    public long pendientes() {
        return recomendacionRepository.findByEstadoOrderBySemanaInicioAscTipoHabitacionIdAsc(EstadoRecomendacion.PENDIENTE).size();
    }

    // Próximo lunes a las 06:00 (hora de NY); null si la tarea está apagada (cron "-")
    public LocalDateTime proximaGeneracion() {
        if ("-".equals(cron)) {
            return null;
        }
        LocalDateTime ahora = ahora();
        LocalDateTime lunes = ahora.toLocalDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY)).atTime(6, 0);
        return lunes.isAfter(ahora) ? lunes : lunes.plusWeeks(1);
    }

    // ===== Internos =====
    private RecomendacionPrecio obtenerPendiente(Long id) {
        RecomendacionPrecio r = recomendacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Recommendation not found: " + id));
        if (r.getEstado() == EstadoRecomendacion.PENDIENTE && !r.getSemanaInicio().isAfter(pronosticoService.hoy())) {
            r.setEstado(EstadoRecomendacion.VENCIDA);
            recomendacionRepository.save(r);
        }
        if (r.getEstado() != EstadoRecomendacion.PENDIENTE) {
            throw new IllegalStateException("This recommendation is no longer pending (" + r.getEstado() + ").");
        }
        return r;
    }

    private RecomendacionPrecio decidir(RecomendacionPrecio r, EstadoRecomendacion estado, String quien) {
        r.setEstado(estado);
        r.setFechaDecision(ahora());
        r.setDecididaPor(quien);
        return recomendacionRepository.save(r);
    }

    private void vencerPendientes(LocalDate hoy) {
        for (RecomendacionPrecio r : recomendacionRepository
                .findByEstadoAndSemanaInicioLessThanEqual(EstadoRecomendacion.PENDIENTE, hoy)) {
            r.setEstado(EstadoRecomendacion.VENCIDA);
            recomendacionRepository.save(r);
        }
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(java.time.ZoneId.of("America/New_York"));
    }

    static String motivo(Temporada temporada, double factorTemporada, double ocupacion) {
        String ocupacionTexto = String.format(Locale.US, "%d%% expected occupancy", Math.round(ocupacion * 100));
        if (temporada == null || Math.abs(factorTemporada - 1) < 0.005) {
            return "No special season · " + ocupacionTexto;
        }
        return String.format(Locale.US, "%s x%.2f · %s", temporada.getNombre(), factorTemporada, ocupacionTexto);
    }

    static String etiqueta(double factorTemporada, double factorDemanda, double factorRecomendado) {
        if (factorRecomendado < 1.0) {
            return "Special rate";
        }
        if (factorTemporada > 1.0 && factorTemporada >= factorDemanda) {
            return "High season";
        }
        if (factorDemanda > 1.0) {
            return "High demand";
        }
        return factorTemporada > 1.0 ? "High season" : null;
    }
}
