package com.theroyale.backend.service.plan;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theroyale.backend.config.GeminiProperties;
import com.theroyale.backend.config.PlanVisitaProperties;
import com.theroyale.backend.dto.CuentaResponse;
import com.theroyale.backend.dto.plan.AgregarBloqueRequest;
import com.theroyale.backend.dto.plan.AjustarPlanRequest;
import com.theroyale.backend.dto.plan.BloquePlanResponse;
import com.theroyale.backend.dto.plan.DiaPlanResponse;
import com.theroyale.backend.dto.plan.EntendidoItem;
import com.theroyale.backend.dto.plan.GenerarPlanRequest;
import com.theroyale.backend.dto.plan.InterpretacionPlan;
import com.theroyale.backend.dto.plan.InterpretarPlanRequest;
import com.theroyale.backend.dto.plan.PlanVisitaResponse;
import com.theroyale.backend.dto.plan.PreferenciasPlan;
import com.theroyale.backend.dto.plan.RegenerarDiaRequest;
import com.theroyale.backend.errors.AccesoDenegadoException;
import com.theroyale.backend.errors.LimiteExcedidoException;
import com.theroyale.backend.errors.RecursoNoEncontradoException;
import com.theroyale.backend.errors.ServicioNoDisponibleException;
import com.theroyale.backend.model.EstadoReserva;
import com.theroyale.backend.model.EtiquetaPlato;
import com.theroyale.backend.model.FuentePlan;
import com.theroyale.backend.model.PlanVisita;
import com.theroyale.backend.model.Reserva;
import com.theroyale.backend.model.TipoBloque;
import com.theroyale.backend.repository.PlanVisitaRepository;
import com.theroyale.backend.repository.ReservaRepository;
import com.theroyale.backend.service.CuentaService;
import com.theroyale.backend.service.chat.LimiteMensajesService;
import com.theroyale.backend.service.plan.fuentes.ClimaDia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

// ===== Plan de visita de una reserva: orquesta, valida reglas de la reserva, guarda y responde =====
// Reglas (sección 10 y contrato): la reserva es del cliente (403), está PENDIENTE, CONFIRMADA o EN_CURSO
// (409), dura hasta 14 noches (400) y admite 5 generaciones por día entre generar, ajustar y regenerar
// un día (429). En EN_CURSO solo se planifican los días que faltan. Sin clave de Gemini, 503.
// "Add to my stay" carga el servicio con CuentaService (cantidad 1, operador predeterminado): 409 si la
// cuenta no está abierta. Una tarea diaria (06:00 de Nueva York) actualiza el clima con NWS.
//
// Las generaciones llaman a fuentes externas y a Gemini (de 10 a 25 s): no corren dentro de una
// transacción; solo se guarda al final. La reserva se carga con ReservaRepository (como ResenaService)
// porque ReservaService depende de este servicio para borrar el plan junto con la reserva.
@Service
public class PlanVisitaService {

    public static final int MAXIMO_NOCHES = 14;
    public static final int DESCRIPCION_MINIMA = 10;
    public static final int DESCRIPCION_MAXIMA = 1000;
    public static final int INDICACION_MAXIMA = 200;
    public static final int NOTAS_MAXIMAS = 300;
    public static final int MAXIMO_INTERESES = 5;
    public static final int MAXIMO_DIETAS = 3;

    static final String SIN_PLAN = "No plan yet for this reservation.";
    static final String NO_ES_SUYA = "This reservation does not belong to you.";
    static final String SIN_GEMINI = GeneradorPlanService.MENSAJE_NO_DISPONIBLE;

    private static final List<EstadoReserva> ESTADOS_PLANIFICABLES =
            List.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA, EstadoReserva.EN_CURSO);
    private static final List<EtiquetaPlato> DIETAS_PERMITIDAS =
            List.of(EtiquetaPlato.VEGANO, EtiquetaPlato.VEGETARIANO, EtiquetaPlato.SIN_GLUTEN);
    private static final Logger log = LoggerFactory.getLogger(PlanVisitaService.class);

    @Autowired
    private PlanVisitaRepository planVisitaRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private ContextoPlanService contextoPlanService;

    @Autowired
    private GeneradorPlanService generadorPlanService;

    @Autowired
    private ValidadorPlanService validadorPlanService;

    @Autowired
    private InterpretePlanService interpretePlanService;

    @Autowired
    private CuentaService cuentaService;

    @Autowired
    private LimiteMensajesService limiteMensajesService;

    @Autowired
    private GeminiProperties geminiProperties;

    @Autowired
    private PlanVisitaProperties propiedades;

    @Autowired
    private ObjectMapper objectMapper;

    // "Hoy" en Nueva York: decide el límite diario y qué días quedan en una estadía EN_CURSO
    private Clock reloj = Clock.system(ContextoPlanService.ZONA_NUEVA_YORK);

    // Para las pruebas
    void setReloj(Clock reloj) {
        this.reloj = reloj;
    }

    public boolean estaDisponible() {
        return geminiProperties.tieneClave();
    }

    // ===== POST /plan/interpretar: texto libre -> preferencias + "What I understood" =====
    // Usa el límite por IP del chat, no el de 5 generaciones por día.
    public InterpretacionPlan interpretar(Long reservaId, InterpretarPlanRequest solicitud, String ip) {
        if (solicitud == null || solicitud.clienteId() == null) {
            throw new IllegalArgumentException("Some required data is missing to read your description.");
        }
        String descripcion = solicitud.descripcion() == null ? "" : solicitud.descripcion().trim();
        if (descripcion.length() < DESCRIPCION_MINIMA || descripcion.length() > DESCRIPCION_MAXIMA) {
            throw new IllegalArgumentException("Tell us about your trip in " + DESCRIPCION_MINIMA + " to "
                    + DESCRIPCION_MAXIMA + " characters.");
        }
        Reserva reserva = reservaPlanificable(reservaId, solicitud.clienteId());
        exigirGemini();
        limiteMensajesService.registrar(ip);
        return interpretePlanService.interpretar(descripcion, DatosEstadia.desde(reserva));
    }

    // ===== POST /plan: genera el plan o reemplaza el que haya =====
    public PlanVisitaResponse generar(Long reservaId, GenerarPlanRequest solicitud) {
        if (solicitud == null || solicitud.clienteId() == null || solicitud.preferencias() == null) {
            throw new IllegalArgumentException("Some required data is missing to build your plan.");
        }
        PreferenciasPlan preferencias = validarPreferencias(solicitud.preferencias());
        String descripcion = textoOpcional(solicitud.descripcion(), DESCRIPCION_MAXIMA, "Your description");
        List<EntendidoItem> entendido = solicitud.entendido() == null ? List.of()
                : solicitud.entendido().stream().filter(Objects::nonNull).limit(5).toList();

        Reserva reserva = reservaPlanificable(reservaId, solicitud.clienteId());
        PlanVisita existente = planVisitaRepository.findByReservaId(reservaId).orElse(null);
        exigirGemini();
        exigirCupo(existente);

        DatosEstadia estadia = DatosEstadia.desde(reserva);
        List<LocalDate> dias = diasAPlanificar(estadia);
        ContextoPlan contexto = contextoPlanService.construir(dias, preferencias);
        GeneradorPlanService.Solicitud pedido = new GeneradorPlanService.Solicitud(estadia, preferencias, descripcion,
                null, dias, null, null);
        ValidadorPlanService.Resultado resultado = validadorPlanService.generarValidado(
                () -> generadorPlanService.generar(contexto, pedido), contexto, estadia, dias, preferencias);

        ContenidoPlan contenido = new ContenidoPlan(resultado.resumen(), entendido, resultado.dias(), List.of());
        PlanVisita plan = existente != null ? existente : PlanVisita.builder().reserva(reserva).build();
        plan.setDescripcion(descripcion);
        plan.setPreferencias(json(preferencias));
        guardarGeneracion(plan, contenido, contexto.fuentesUsadas(), contexto.fuentesNoDisponibles(), resultado.modelo(), true);
        return responder(reserva, plan);
    }

    // ===== GET /plan =====
    public PlanVisitaResponse obtener(Long reservaId, Long clienteId) {
        Reserva reserva = reservaPropia(reservaId, clienteId);
        return responder(reserva, planExistente(reservaId));
    }

    // ===== POST /plan/ajustar: rehace el plan entero con la indicación y el plan actual como contexto =====
    public PlanVisitaResponse ajustar(Long reservaId, AjustarPlanRequest solicitud) {
        if (solicitud == null || solicitud.clienteId() == null) {
            throw new IllegalArgumentException("Some required data is missing to adjust your plan.");
        }
        String indicacion = solicitud.indicacion() == null ? "" : solicitud.indicacion().trim();
        if (indicacion.isEmpty() || indicacion.length() > INDICACION_MAXIMA) {
            throw new IllegalArgumentException("Tell the concierge what to change in 1 to " + INDICACION_MAXIMA + " characters.");
        }
        Reserva reserva = reservaPlanificable(reservaId, solicitud.clienteId());
        PlanVisita plan = planExistente(reservaId);
        exigirGemini();
        exigirCupo(plan);

        ContenidoPlan actual = leerContenido(plan);
        PreferenciasPlan preferencias = leerPreferencias(plan);
        DatosEstadia estadia = DatosEstadia.desde(reserva);
        List<LocalDate> dias = diasAPlanificar(estadia);
        ContextoPlan contexto = contextoPlanService.construir(dias, preferencias);
        GeneradorPlanService.Solicitud pedido = new GeneradorPlanService.Solicitud(estadia, preferencias,
                plan.getDescripcion(), indicacion, dias, resumirDias(actual.dias(), null), null);
        ValidadorPlanService.Resultado resultado = validadorPlanService.generarValidado(
                () -> generadorPlanService.generar(contexto, pedido), contexto, estadia, dias, preferencias);

        ContenidoPlan contenido = new ContenidoPlan(resultado.resumen().isEmpty() ? actual.resumen() : resultado.resumen(),
                actual.entendido(), resultado.dias(), List.of());
        guardarGeneracion(plan, contenido, contexto.fuentesUsadas(), contexto.fuentesNoDisponibles(), resultado.modelo(), true);
        return responder(reserva, plan);
    }

    // ===== POST /plan/dias/{fecha}/regenerar: cambia solo ese día y deja los demás iguales =====
    public PlanVisitaResponse regenerarDia(Long reservaId, LocalDate fecha, RegenerarDiaRequest solicitud) {
        if (solicitud == null || solicitud.clienteId() == null || fecha == null) {
            throw new IllegalArgumentException("Some required data is missing to rebuild this day.");
        }
        String indicacion = textoOpcional(solicitud.indicacion(), INDICACION_MAXIMA, "The instruction");
        Reserva reserva = reservaPlanificable(reservaId, solicitud.clienteId());
        PlanVisita plan = planExistente(reservaId);
        ContenidoPlan actual = leerContenido(plan);
        if (actual.dias().stream().noneMatch(dia -> dia.fecha().equals(fecha))) {
            throw new IllegalArgumentException("That day is not part of your plan.");
        }
        if (fecha.isBefore(LocalDate.now(reloj))) {
            throw new IllegalArgumentException("That day has already passed.");
        }
        exigirGemini();
        exigirCupo(plan);

        PreferenciasPlan preferencias = leerPreferencias(plan);
        DatosEstadia estadia = DatosEstadia.desde(reserva);
        List<LocalDate> dias = List.of(fecha);
        ContextoPlan contexto = contextoPlanService.construir(dias, preferencias);
        GeneradorPlanService.Solicitud pedido = new GeneradorPlanService.Solicitud(estadia, preferencias,
                plan.getDescripcion(), indicacion, dias, resumirDias(actual.dias(), fecha), resumirDias(actual.dias(), null, fecha));
        ValidadorPlanService.Resultado resultado = validadorPlanService.generarValidado(
                () -> generadorPlanService.generar(contexto, pedido), contexto, estadia, dias, preferencias);

        DiaPlanResponse nuevo = resultado.dias().get(0);
        List<DiaPlanResponse> diasNuevos = actual.dias().stream()
                .map(dia -> dia.fecha().equals(fecha) ? nuevo : dia)
                .toList();
        List<LocalDate> enRevision = actual.diasEnRevision() == null ? List.of()
                : actual.diasEnRevision().stream().filter(d -> !d.equals(fecha)).toList();
        ContenidoPlan contenido = new ContenidoPlan(actual.resumen(), actual.entendido(), diasNuevos, enRevision);

        // Las fuentes del plan suman las de esta generación; deja de estar caída la que ahora respondió
        Set<FuentePlan> usadas = new TreeSet<>(leerFuentes(plan.getFuentesUsadas()));
        usadas.addAll(contexto.fuentesUsadas());
        Set<FuentePlan> caidas = new TreeSet<>(leerFuentes(plan.getFuentesNoDisponibles()));
        caidas.addAll(contexto.fuentesNoDisponibles());
        caidas.removeAll(contexto.fuentesUsadas());
        usadas.removeAll(caidas);
        guardarGeneracion(plan, contenido, new ArrayList<>(usadas), new ArrayList<>(caidas), resultado.modelo(), false);
        return responder(reserva, plan);
    }

    // ===== POST /plan/bloques/{bloqueId}/agregar-a-cuenta ("Add to my stay") =====
    public CuentaResponse agregarACuenta(Long reservaId, String bloqueId, AgregarBloqueRequest solicitud) {
        if (solicitud == null || solicitud.clienteId() == null) {
            throw new IllegalArgumentException("Some required data is missing to add this to your stay.");
        }
        reservaPropia(reservaId, solicitud.clienteId());
        ContenidoPlan contenido = leerContenido(planExistente(reservaId));
        BloquePlanResponse bloque = contenido.dias().stream()
                .flatMap(dia -> dia.bloques().stream())
                .filter(b -> b.id().equals(bloqueId))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Plan item not found: " + bloqueId));
        if (bloque.tipo() != TipoBloque.SERVICIO_HOTEL || bloque.servicioId() == null) {
            throw new IllegalArgumentException("Only hotel services can be added to your stay.");
        }
        // Operador null: CuentaService usa el predeterminado. 409 si la cuenta no está abierta (PENDIENTE)
        return cuentaService.agregarConsumo(reservaId, bloque.servicioId(), 1, null);
    }

    // ===== DELETE /plan =====
    @Transactional
    public void eliminar(Long reservaId, Long clienteId) {
        Reserva reserva = reservaPropia(reservaId, clienteId);
        PlanVisita plan = planExistente(reservaId);
        reserva.setPlanVisita(null);
        planVisitaRepository.delete(plan);
    }

    // ===== La dispara ReservaService.eliminar antes de borrar la reserva =====
    // Se hace flush para que el DELETE del plan salga antes que el de la reserva (FK reserva_id).
    @Transactional
    public void eliminarPorReserva(Reserva reserva) {
        planVisitaRepository.findByReservaId(reserva.getId()).ifPresent(plan -> {
            reserva.setPlanVisita(null);
            planVisitaRepository.delete(plan);
            planVisitaRepository.flush();
        });
    }

    // ===== Tarea diaria: vuelve a pedir el pronóstico de NWS para las estadías que llegan en 7 días =====
    // Actualiza el clima de cada día y marca requiereRevision si un día con bloques al aire libre pasa a
    // tener 60 % o más de lluvia. No regenera nada: el huésped decide. "-" en plan.clima.cron la apaga.
    @Scheduled(cron = "${plan.clima.cron:0 0 6 * * *}", zone = "America/New_York")
    @Transactional
    public void actualizarClima() {
        LocalDate hoy = LocalDate.now(reloj);
        LocalDate hasta = hoy.plusDays(ContextoPlanService.DIAS_NWS - 1L);
        List<PlanVisita> planes = planVisitaRepository.buscarConDiasEntre(hoy, hasta, ESTADOS_PLANIFICABLES);
        if (planes.isEmpty()) {
            return;
        }
        Map<LocalDate, ClimaDia> pronostico;
        try {
            pronostico = contextoPlanService.pronosticoNws(hoy, hasta);
        } catch (RuntimeException ex) {
            log.warn("No se pudo actualizar el clima de los planes: {}", ex.getMessage());
            return;
        }

        for (PlanVisita plan : planes) {
            ContenidoPlan contenido = leerContenido(plan);
            Set<LocalDate> enRevision = new LinkedHashSet<>(contenido.diasEnRevision() == null ? List.of() : contenido.diasEnRevision());
            boolean cambio = false;
            List<DiaPlanResponse> dias = new ArrayList<>();
            for (DiaPlanResponse dia : contenido.dias()) {
                ClimaDia nuevo = pronostico.get(dia.fecha());
                if (nuevo == null) {
                    dias.add(dia);
                    continue;
                }
                int lluviaAnterior = dia.clima() == null || dia.clima().probLluvia() == null ? 0 : dia.clima().probLluvia();
                boolean alAireLibre = dia.bloques().stream().anyMatch(b ->
                        (b.tipo() == TipoBloque.LUGAR || b.tipo() == TipoBloque.EVENTO) && Boolean.FALSE.equals(b.bajoTecho()));
                if (nuevo.probLluviaOCero() >= ValidadorPlanService.LLUVIA_SOLO_BAJO_TECHO
                        && lluviaAnterior < ValidadorPlanService.LLUVIA_SOLO_BAJO_TECHO && alAireLibre) {
                    enRevision.add(dia.fecha());
                }
                dias.add(dia.conClima(ValidadorPlanService.climaRespuesta(nuevo)));
                cambio = true;
            }
            if (cambio) {
                List<LocalDate> revision = enRevision.stream().sorted().toList();
                plan.setContenido(json(new ContenidoPlan(contenido.resumen(), contenido.entendido(), dias, revision)));
                plan.setRequiereRevision(!revision.isEmpty());
                plan.setFechaActualizacion(ahora());
                planVisitaRepository.save(plan);
            }
        }
        log.info("Clima actualizado en {} planes de visita", planes.size());
    }

    // ===== Plan de ejemplo ya validado (DataLoader), sin claves ni internet =====
    @Transactional
    public void sembrarPlanDeEjemplo(Reserva reserva, PreferenciasPlan preferencias, String descripcion,
                                     List<EntendidoItem> entendido, PlanGenerado propuesto) {
        DatosEstadia estadia = DatosEstadia.desde(reserva);
        List<LocalDate> dias = diasAPlanificar(estadia);
        ContextoPlan contexto = contextoPlanService.construirSinFuentes(dias, preferencias);
        ValidadorPlanService.Resultado resultado = validadorPlanService.validar(propuesto, contexto, estadia, dias,
                preferencias, "example (DataLoader)");
        PlanVisita plan = PlanVisita.builder()
                .reserva(reserva)
                .preferencias(json(preferencias))
                .descripcion(descripcion)
                .build();
        guardarGeneracion(plan, new ContenidoPlan(resultado.resumen(), entendido, resultado.dias(), List.of()),
                List.of(), List.of(), resultado.modelo(), false);
        reserva.setPlanVisita(plan);
    }

    // ===== Reglas de la reserva =====
    private Reserva reservaPropia(Long reservaId, Long clienteId) {
        if (clienteId == null) {
            throw new IllegalArgumentException("Some required data is missing.");
        }
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada: " + reservaId));
        if (reserva.getCliente() == null || !reserva.getCliente().getId().equals(clienteId)) {
            throw new AccesoDenegadoException(NO_ES_SUYA);
        }
        return reserva;
    }

    private Reserva reservaPlanificable(Long reservaId, Long clienteId) {
        Reserva reserva = reservaPropia(reservaId, clienteId);
        if (!ESTADOS_PLANIFICABLES.contains(reserva.getEstado())) {
            throw new IllegalStateException("Only pending, confirmed or in-progress reservations can have a visit plan.");
        }
        if (reserva.getNoches() > MAXIMO_NOCHES) {
            throw new IllegalArgumentException("Visit plans are available for stays of up to " + MAXIMO_NOCHES + " nights.");
        }
        if (reserva.getFechaFin().isBefore(LocalDate.now(reloj))) {
            throw new IllegalStateException("This stay has already ended.");
        }
        return reserva;
    }

    private PlanVisita planExistente(Long reservaId) {
        return planVisitaRepository.findByReservaId(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(SIN_PLAN));
    }

    private void exigirGemini() {
        if (!estaDisponible()) {
            throw new ServicioNoDisponibleException(SIN_GEMINI);
        }
    }

    private void exigirCupo(PlanVisita plan) {
        if (generacionesUsadasHoy(plan) >= propiedades.getMaxGeneracionesDia()) {
            throw new LimiteExcedidoException("You have reached today's limit of " + propiedades.getMaxGeneracionesDia()
                    + " plans for this stay. Try again tomorrow.");
        }
    }

    private int generacionesUsadasHoy(PlanVisita plan) {
        if (plan == null || plan.getFechaUltimaGeneracion() == null
                || !plan.getFechaUltimaGeneracion().equals(LocalDate.now(reloj))) {
            return 0;
        }
        return plan.getGeneracionesHoy();
    }

    // En EN_CURSO solo los días que faltan (desde hoy); en las demás, toda la estadía
    private List<LocalDate> diasAPlanificar(DatosEstadia estadia) {
        LocalDate hoy = LocalDate.now(reloj);
        LocalDate desde = estadia.fechaInicio().isBefore(hoy) ? hoy : estadia.fechaInicio();
        List<LocalDate> dias = new ArrayList<>();
        for (LocalDate dia = desde; !dia.isAfter(estadia.fechaFin()); dia = dia.plusDays(1)) {
            dias.add(dia);
        }
        return dias;
    }

    // ===== Validación de las preferencias (400) =====
    PreferenciasPlan validarPreferencias(PreferenciasPlan p) {
        if (p.intereses() == null || p.intereses().isEmpty() || p.intereses().size() > MAXIMO_INTERESES
                || p.intereses().stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Choose between 1 and " + MAXIMO_INTERESES + " interests.");
        }
        if (new HashSet<>(p.intereses()).size() != p.intereses().size()) {
            throw new IllegalArgumentException("Each interest can be chosen only once.");
        }
        if (p.ritmo() == null) {
            throw new IllegalArgumentException("Choose a pace for your days.");
        }
        if (p.presupuesto() == null) {
            throw new IllegalArgumentException("Choose a budget.");
        }
        List<EtiquetaPlato> dieta = p.dieta() == null ? List.of() : p.dieta();
        if (dieta.size() > MAXIMO_DIETAS || dieta.stream().anyMatch(Objects::isNull) || !DIETAS_PERMITIDAS.containsAll(dieta)
                || new HashSet<>(dieta).size() != dieta.size()) {
            throw new IllegalArgumentException("Diet options are VEGANO, VEGETARIANO and SIN_GLUTEN, each at most once.");
        }
        String notas = textoOpcional(p.notas(), NOTAS_MAXIMAS, "Notes");
        return new PreferenciasPlan(List.copyOf(p.intereses()), p.ritmo(), p.presupuesto(), List.copyOf(dieta),
                p.movilidadReducida(), p.conNinos(), notas);
    }

    private static String textoOpcional(String texto, int maximo, String campo) {
        if (texto == null || texto.trim().isEmpty()) {
            return null;
        }
        if (texto.trim().length() > maximo) {
            throw new IllegalArgumentException(campo + " cannot exceed " + maximo + " characters.");
        }
        return texto.trim();
    }

    // ===== Resumen del plan guardado para darle contexto a Gemini ("10:00-12:30 LUGAR The Met") =====
    private Map<String, List<String>> resumirDias(List<DiaPlanResponse> dias, LocalDate soloEste, LocalDate... excluir) {
        List<LocalDate> excluidos = Arrays.asList(excluir);
        Map<String, List<String>> resumen = new LinkedHashMap<>();
        for (DiaPlanResponse dia : dias) {
            if ((soloEste != null && !dia.fecha().equals(soloEste)) || excluidos.contains(dia.fecha())) {
                continue;
            }
            resumen.put(dia.fecha().toString(), dia.bloques().stream()
                    .filter(b -> b.tipo() != TipoBloque.TIEMPO_LIBRE)
                    .map(b -> b.inicio() + "-" + b.fin() + " " + b.tipo() + " " + b.titulo())
                    .toList());
        }
        return resumen;
    }

    // ===== Guardado y respuesta =====
    private void guardarGeneracion(PlanVisita plan, ContenidoPlan contenido, List<FuentePlan> usadas,
                                   List<FuentePlan> noDisponibles, String modelo, boolean planCompleto) {
        LocalDateTime ahora = ahora();
        LocalDate hoy = LocalDate.now(reloj);
        if (plan.getFechaGeneracion() == null || planCompleto) {
            plan.setFechaGeneracion(ahora);
        }
        plan.setFechaActualizacion(ahora);
        plan.setContenido(json(contenido));
        plan.setFuentesUsadas(unir(usadas));
        plan.setFuentesNoDisponibles(unir(noDisponibles));
        plan.setRequiereRevision(contenido.diasEnRevision() != null && !contenido.diasEnRevision().isEmpty());
        plan.setModeloIa(modelo);
        // El plan de ejemplo del DataLoader no gasta cupo (modelo "example")
        if (modelo != null && !modelo.startsWith("example")) {
            plan.setGeneracionesHoy(generacionesUsadasHoy(plan) + 1);
            plan.setFechaUltimaGeneracion(hoy);
        }
        planVisitaRepository.save(plan);
    }

    PlanVisitaResponse responder(Reserva reserva, PlanVisita plan) {
        ContenidoPlan contenido = leerContenido(plan);
        boolean cuentaAbierta = reserva.getEstado() == EstadoReserva.CONFIRMADA || reserva.getEstado() == EstadoReserva.EN_CURSO;
        List<DiaPlanResponse> dias = contenido.dias().stream()
                .map(dia -> dia.conBloques(dia.bloques().stream().map(b -> b.conAgregable(cuentaAbierta)).toList()))
                .toList();
        String tipo = reserva.getHabitacion() == null || reserva.getHabitacion().getTipoHabitacion() == null
                ? null : reserva.getHabitacion().getTipoHabitacion().getNombre();
        int restantes = Math.max(0, propiedades.getMaxGeneracionesDia() - generacionesUsadasHoy(plan));
        return new PlanVisitaResponse(reserva.getId(), reserva.getEstado(), reserva.getFechaInicio(), reserva.getFechaFin(),
                reserva.getNoches(), tipo, reserva.getCantidadPersonas(), contenido.resumen(), plan.getDescripcion(),
                contenido.entendido() == null ? List.of() : contenido.entendido(), leerPreferencias(plan),
                plan.isRequiereRevision(), contenido.diasEnRevision() == null ? List.of() : contenido.diasEnRevision(),
                leerFuentes(plan.getFuentesUsadas()), leerFuentes(plan.getFuentesNoDisponibles()), restantes,
                plan.getFechaGeneracion(), plan.getFechaActualizacion(), dias);
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(reloj).truncatedTo(ChronoUnit.SECONDS);
    }

    private static String unir(List<FuentePlan> fuentes) {
        return String.join(",", fuentes.stream().distinct().map(Enum::name).toList());
    }

    private static List<FuentePlan> leerFuentes(String texto) {
        if (texto == null || texto.isBlank()) {
            return List.of();
        }
        return Arrays.stream(texto.split(",")).map(String::trim).filter(s -> !s.isEmpty()).map(FuentePlan::valueOf).toList();
    }

    private ContenidoPlan leerContenido(PlanVisita plan) {
        try {
            return objectMapper.readValue(plan.getContenido(), ContenidoPlan.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("The saved plan could not be read.", ex);
        }
    }

    private PreferenciasPlan leerPreferencias(PlanVisita plan) {
        try {
            return objectMapper.readValue(plan.getPreferencias(), PreferenciasPlan.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("The saved preferences could not be read.", ex);
        }
    }

    private String json(Object valor) {
        try {
            return objectMapper.writeValueAsString(valor);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo guardar el plan", ex);
        }
    }
}
