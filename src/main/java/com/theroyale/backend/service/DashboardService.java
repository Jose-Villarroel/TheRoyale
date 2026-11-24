package com.theroyale.backend.service;

import com.theroyale.backend.dto.dashboard.AlertaResponse;
import com.theroyale.backend.dto.dashboard.DesgloseResponse;
import com.theroyale.backend.dto.dashboard.FilaDesgloseResponse;
import com.theroyale.backend.dto.dashboard.HabitacionMapaResponse;
import com.theroyale.backend.dto.dashboard.KpisResponse;
import com.theroyale.backend.dto.dashboard.OperacionHoyResponse;
import com.theroyale.backend.dto.dashboard.PuntoIngresosResponse;
import com.theroyale.backend.dto.dashboard.PuntoOcupacionResponse;
import com.theroyale.backend.dto.dashboard.RatingTipoResponse;
import com.theroyale.backend.dto.dashboard.ReputacionResponse;
import com.theroyale.backend.dto.dashboard.ResenaRecienteResponse;
import com.theroyale.backend.dto.dashboard.ResumenResponse;
import com.theroyale.backend.dto.dashboard.SaldoPendienteResponse;
import com.theroyale.backend.model.EstadoCuenta;
import com.theroyale.backend.model.EstadoHabitacion;
import com.theroyale.backend.model.EstadoReserva;
import com.theroyale.backend.model.Habitacion;
import com.theroyale.backend.model.PlanVisita;
import com.theroyale.backend.model.Resena;
import com.theroyale.backend.model.Reserva;
import com.theroyale.backend.repository.CuentaRepository;
import com.theroyale.backend.repository.HabitacionRepository;
import com.theroyale.backend.repository.ItemConsumoRepository;
import com.theroyale.backend.repository.PagoRepository;
import com.theroyale.backend.repository.PlanVisitaRepository;
import com.theroyale.backend.repository.ResenaRepository;
import com.theroyale.backend.repository.ReservaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

// =============================================================================================
// ===== Agregación de datos para el dashboard de dirección del administrador =====
//
// Toda la suma ocurre en la base de datos (SUM, COUNT, AVG, GROUP BY en los repositorios). Este
// servicio solo compone: convierte las filas agregadas en DTOs, aplica las divisiones de los
// indicadores y decide qué alertas merecen salir. En ningún método se recorren todas las reservas
// ni todos los consumos para sumarlos en memoria.
//
// Las dos únicas consultas que devuelven entidades completas lo hacen porque necesitan el detalle,
// no un total: el mapa de habitaciones (estado de cada una) y las reseñas recientes.
//
// ===== Definiciones de negocio (las cifras del panel significan exactamente esto) =====
//
// RN-1  Noche-habitación vendida: cada noche entre fechaInicio (incluida) y fechaFin (excluida) de
//       una reserva CONFIRMADA, EN_CURSO o FINALIZADA. PENDIENTE y CANCELADA no se venden.
// RN-2  Habitaciones del inventario: las que no están DESHABILITADA. MANTENIMIENTO sí cuenta como
//       inventario y se informa aparte, porque es temporal y esconderla inflaría la ocupación.
// RN-3  Ocupación % = noches vendidas / (inventario x días del período).
// RN-4  ADR = ingresos de habitación / noches vendidas.
// RN-5  RevPAR = ingresos de habitación / (inventario x días del período).
// RN-6  Ingresos de habitación = suma de noches x precioNocheAcordado, recortadas al período.
// RN-7  Ingresos por servicios = suma de cantidad x precioUnitario, imputados por fechaHora.
// RN-8  Cobrado (caja) = suma de Pago.monto, imputado por fecha. Facturado y cobrado se muestran
//       SIEMPRE por separado: no son lo mismo y confundirlos da por ingresado lo que no se ha cobrado.
// RN-9  Saldo pendiente = consumos - pagos, solo en cuentas ABIERTA.
// RN-10 Tasa de cancelación = canceladas / creadas en el período, ambas por fechaCreacion.
// RN-11 Lead time = promedio de días entre fechaCreacion y fechaInicio.
// RN-12 ALOS = promedio de noches por reserva.
// RN-13 Ingreso por huésped = ingresos totales / reservas atendidas.
// RN-14 Rating = promedio de Resena.calificacion con visible = true, más el número de reseñas.
// RN-15 Período anterior = mismo número de días inmediatamente antes de "desde".
// =============================================================================================
@Service
@Transactional(readOnly = true)
public class DashboardService {

    // RN-1: lo que cuenta como vendido
    private static final List<EstadoReserva> ESTADOS_VENDIBLES =
            List.of(EstadoReserva.CONFIRMADA, EstadoReserva.EN_CURSO, EstadoReserva.FINALIZADA);

    // Llega hoy quien tiene reserva confirmada o ya hizo el check-in
    private static final List<EstadoReserva> ESTADOS_LLEGADA =
            List.of(EstadoReserva.CONFIRMADA, EstadoReserva.EN_CURSO);

    // Sale hoy quien está en casa o ya cerró su estadía
    private static final List<EstadoReserva> ESTADOS_SALIDA =
            List.of(EstadoReserva.EN_CURSO, EstadoReserva.FINALIZADA);

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final int ESCALA = 2;

    // El panel enseña un mes por defecto: suficiente para ver tendencia sin que la consulta pese
    private static final int DIAS_PERIODO_POR_DEFECTO = 30;

    // Un rango mayor a un año no cabe legiblemente en ninguna gráfica del panel
    private static final int MAX_DIAS_PERIODO = 366;

    // Ventana de "ya reservado" que la gráfica de ocupación muestra hacia el futuro
    private static final int DIAS_ON_THE_BOOKS = 30;

    // Umbrales de las alertas
    private static final int HORAS_PARA_CONFIRMAR = 48;
    private static final int CALIFICACION_NEGATIVA_MAXIMA = 2;
    private static final int DIAS_RESENAS_RECIENTES = 7;
    private static final BigDecimal OCUPACION_ALTA = BigDecimal.valueOf(85);
    private static final BigDecimal CAIDA_RELEVANTE = BigDecimal.valueOf(15);
    private static final int DIAS_PLANES_PROXIMOS = 14;
    private static final int MAX_RESENAS_RECIENTES = 5;

    private static final String SEVERIDAD_ALTA = "ALTA";
    private static final String SEVERIDAD_MEDIA = "MEDIA";
    private static final String SEVERIDAD_BAJA = "BAJA";

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private HabitacionRepository habitacionRepository;

    @Autowired
    private CuentaRepository cuentaRepository;

    @Autowired
    private ItemConsumoRepository itemConsumoRepository;

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private ResenaRepository resenaRepository;

    @Autowired
    private PlanVisitaRepository planVisitaRepository;

    // =========================================================================================
    // ===== GET /resumen =====
    // =========================================================================================

    public ResumenResponse resumen(LocalDate desde, LocalDate hasta) {
        validarPeriodo(desde, hasta);
        int dias = diasDe(desde, hasta);

        // RN-15: el período anterior equivalente termina el día antes de que empiece el actual
        LocalDate hastaAnterior = desde.minusDays(1);
        LocalDate desdeAnterior = hastaAnterior.minusDays(dias - 1L);

        return new ResumenResponse(
                desde, hasta,
                desdeAnterior, hastaAnterior,
                calcularKpis(desde, hasta),
                calcularKpis(desdeAnterior, hastaAnterior),
                LocalDateTime.now());
    }

    private KpisResponse calcularKpis(LocalDate desde, LocalDate hasta) {
        LocalDate hastaExclusivo = hasta.plusDays(1);
        int dias = diasDe(desde, hasta);
        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime finExclusivo = hastaExclusivo.atStartOfDay();

        // RN-1 y RN-6 en una sola pasada
        Object[] nochesIngresos = primeraFila(reservaRepository.nochesEIngresosHabitacion(desde, hastaExclusivo));
        long nochesVendidas = comoLong(nochesIngresos[0]);
        BigDecimal ingresosHabitaciones = comoBigDecimal(nochesIngresos[1]);

        BigDecimal ingresosServicios = itemConsumoRepository.sumarIngresosServicios(inicio, finExclusivo); // RN-7
        BigDecimal cobrado = pagoRepository.sumarCobrado(inicio, finExclusivo);                            // RN-8
        BigDecimal ingresosTotales = ingresosHabitaciones.add(ingresosServicios);

        // RN-2: el inventario es el denominador de ocupación y RevPAR
        long inventario = habitacionRepository.contarInventario(EstadoHabitacion.DESHABILITADA);
        long mantenimiento = habitacionRepository.countByEstado(EstadoHabitacion.MANTENIMIENTO);
        BigDecimal nochesDisponibles = BigDecimal.valueOf(inventario).multiply(BigDecimal.valueOf(dias));

        BigDecimal ocupacion = porcentaje(BigDecimal.valueOf(nochesVendidas), nochesDisponibles);   // RN-3
        BigDecimal adr = dividir(ingresosHabitaciones, BigDecimal.valueOf(nochesVendidas));         // RN-4
        BigDecimal revpar = dividir(ingresosHabitaciones, nochesDisponibles);                       // RN-5

        BigDecimal saldoPendiente = saldoPendienteTotal();                                          // RN-9

        // RN-10: ambas por fechaCreacion, para medir lo que se cayó de lo que se vendió esos días
        long creadas = reservaRepository.countByFechaCreacionBetween(inicio, finDeDia(hasta));
        long canceladas = reservaRepository.countByEstadoAndFechaCreacionBetween(
                EstadoReserva.CANCELADA, inicio, finDeDia(hasta));
        BigDecimal tasaCancelacion = porcentaje(BigDecimal.valueOf(canceladas), BigDecimal.valueOf(creadas));

        // RN-11, RN-12 y las reservas atendidas del período
        Object[] promedios = primeraFila(reservaRepository.promediosDeReserva(desde, hastaExclusivo));
        BigDecimal leadTime = redondear(comoBigDecimal(promedios[0]));
        BigDecimal alos = redondear(comoBigDecimal(promedios[1]));
        long reservasAtendidas = comoLong(promedios[2]);

        BigDecimal ingresoPorHuesped = dividir(ingresosTotales, BigDecimal.valueOf(reservasAtendidas)); // RN-13

        // RN-14, acotado al período para que la tarjeta pueda compararse con el anterior.
        // Si en el período nadie escribió, ratingPromedio va en null y la tarjeta muestra un guion:
        // un 0 se leería como "nos calificaron con cero".
        Object[] rating = primeraFila(resenaRepository.resumenEnPeriodo(inicio, finExclusivo));
        long totalResenas = comoLong(rating[1]);
        BigDecimal ratingPromedio = totalResenas == 0 ? null : redondear(comoBigDecimal(rating[0]));

        return new KpisResponse(
                ocupacion, adr, revpar,
                ingresosHabitaciones, ingresosServicios, ingresosTotales,
                cobrado, saldoPendiente,
                nochesVendidas, inventario, mantenimiento, dias,
                ratingPromedio, totalResenas,
                tasaCancelacion, leadTime, alos, ingresoPorHuesped, reservasAtendidas);
    }

    // RN-9: se calcula con dos sumas en la base y una resta aquí
    private BigDecimal saldoPendienteTotal() {
        BigDecimal consumos = itemConsumoRepository.sumarConsumosDeCuentas(EstadoCuenta.ABIERTA);
        BigDecimal pagos = pagoRepository.sumarPagosDeCuentas(EstadoCuenta.ABIERTA);
        return consumos.subtract(pagos);
    }

    // =========================================================================================
    // ===== GET /ingresos-serie =====
    // =========================================================================================

    public List<PuntoIngresosResponse> ingresosSerie(LocalDate desde, LocalDate hasta, String agrupacion) {
        validarPeriodo(desde, hasta);
        String modo = agrupacion == null || agrupacion.isBlank() ? "dia" : agrupacion.trim().toLowerCase(Locale.ROOT);
        if (!List.of("dia", "semana", "mes").contains(modo)) {
            throw new IllegalArgumentException("Grouping must be dia, semana or mes.");
        }

        Map<LocalDate, BigDecimal> habitaciones = new LinkedHashMap<>();
        for (Object[] fila : reservaRepository.serieDiariaHabitaciones(desde, rangoSystemRange(desde, hasta))) {
            habitaciones.put(comoLocalDate(fila[0]), comoBigDecimal(fila[2]));
        }

        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime finExclusivo = hasta.plusDays(1).atStartOfDay();
        Map<LocalDate, BigDecimal> servicios = porDia(itemConsumoRepository.serieDiariaServicios(inicio, finExclusivo));
        Map<LocalDate, BigDecimal> cobrado = porDia(pagoRepository.serieDiariaCobrado(inicio, finExclusivo));

        // Se agrupan los días ya sumados: la base entregó un total por día y aquí solo se juntan
        Map<LocalDate, BigDecimal[]> agrupado = new TreeMap<>();
        for (LocalDate dia : habitaciones.keySet()) {
            LocalDate clave = claveDeAgrupacion(dia, modo);
            BigDecimal[] acumulado = agrupado.computeIfAbsent(clave,
                    k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            acumulado[0] = acumulado[0].add(habitaciones.getOrDefault(dia, BigDecimal.ZERO));
            acumulado[1] = acumulado[1].add(servicios.getOrDefault(dia, BigDecimal.ZERO));
            acumulado[2] = acumulado[2].add(cobrado.getOrDefault(dia, BigDecimal.ZERO));
        }

        List<PuntoIngresosResponse> serie = new ArrayList<>();
        for (Map.Entry<LocalDate, BigDecimal[]> entrada : agrupado.entrySet()) {
            BigDecimal[] valores = entrada.getValue();
            serie.add(new PuntoIngresosResponse(
                    etiquetaDe(entrada.getKey(), modo), entrada.getKey(),
                    valores[0], valores[1], valores[2]));
        }
        return serie;
    }

    // =========================================================================================
    // ===== GET /ocupacion-serie =====
    // =========================================================================================

    public List<PuntoOcupacionResponse> ocupacionSerie(LocalDate desde, LocalDate hasta) {
        validarPeriodo(desde, hasta);
        LocalDate hoy = LocalDate.now();

        // La serie se estira para cubrir los próximos 30 días ("on the books"): el administrador
        // necesita ver lo que ya está reservado, no solo lo que pasó. Pero solo si el período
        // consultado llega hasta hoy; si pidió un rango histórico cerrado (por ejemplo marzo del
        // año pasado) se le devuelve ese rango y nada más, porque estirarlo hasta hoy+30 daría
        // cientos de puntos que nadie pidió y falsearía la escala de la gráfica.
        LocalDate fin = hasta;
        if (!hasta.isBefore(hoy)) {
            LocalDate limiteFuturo = hoy.plusDays(DIAS_ON_THE_BOOKS);
            if (fin.isBefore(limiteFuturo)) {
                fin = limiteFuturo;
            }
        }

        // El inventario es el actual y se aplica a todos los días de la serie. Es una aproximación
        // consciente: el modelo no guarda el histórico de habilitaciones, así que no hay forma de
        // saber cuántas habitaciones existían hace dos meses.
        long inventario = habitacionRepository.contarInventario(EstadoHabitacion.DESHABILITADA);
        BigDecimal inventarioDecimal = BigDecimal.valueOf(inventario);

        List<PuntoOcupacionResponse> serie = new ArrayList<>();
        for (Object[] fila : reservaRepository.serieDiariaHabitaciones(desde, rangoSystemRange(desde, fin))) {
            LocalDate dia = comoLocalDate(fila[0]);
            long noches = comoLong(fila[1]);
            serie.add(new PuntoOcupacionResponse(
                    dia, noches, comoBigDecimal(fila[2]), inventario,
                    porcentaje(BigDecimal.valueOf(noches), inventarioDecimal),
                    dia.isAfter(hoy)));
        }
        return serie;
    }

    // =========================================================================================
    // ===== GET /hoy =====
    // =========================================================================================

    public OperacionHoyResponse operacionHoy() {
        LocalDate hoy = LocalDate.now();

        long llegadas = reservaRepository.contarLlegadas(hoy, ESTADOS_LLEGADA);
        long salidas = reservaRepository.contarSalidas(hoy, ESTADOS_SALIDA);
        long enCasa = reservaRepository.contarEnCasa(EstadoReserva.EN_CURSO, hoy);

        // Solo interesan las que deben algo: una cuenta abierta en cero no requiere atención
        List<SaldoPendienteResponse> saldos = new ArrayList<>();
        for (Object[] fila : cuentaRepository.saldosDeCuentasConSalidaEn(EstadoCuenta.ABIERTA, hoy)) {
            BigDecimal saldo = comoBigDecimal(fila[4]);
            if (saldo.signum() > 0) {
                saldos.add(new SaldoPendienteResponse(
                        comoLong(fila[0]), (String) fila[1],
                        ((String) fila[2]) + " " + ((String) fila[3]), saldo));
            }
        }
        saldos.sort(Comparator.comparing(SaldoPendienteResponse::saldo).reversed());

        List<HabitacionMapaResponse> mapa = new ArrayList<>();
        for (Habitacion habitacion : habitacionRepository.findAllByOrderByNumeroAsc()) {
            mapa.add(new HabitacionMapaResponse(
                    habitacion.getId(), habitacion.getNumero(), pisoDe(habitacion.getNumero()),
                    habitacion.getEstado().name(),
                    habitacion.getTipoHabitacion() == null ? "" : habitacion.getTipoHabitacion().getNombre()));
        }

        return new OperacionHoyResponse(
                hoy, llegadas, salidas, enCasa,
                saldos.size(), saldoPendienteTotal(),
                habitacionRepository.countByEstado(EstadoHabitacion.MANTENIMIENTO),
                habitacionRepository.countByEstado(EstadoHabitacion.DESHABILITADA),
                mapa, saldos);
    }

    // El modelo no guarda el piso: se deduce del número de habitación quitando los dos últimos
    // dígitos, que son el de la puerta (101 es piso 1, 901 piso 9, 1012 piso 10).
    private static String pisoDe(String numero) {
        if (numero == null || numero.length() <= 2) {
            return numero == null ? "" : numero;
        }
        return numero.substring(0, numero.length() - 2);
    }

    // =========================================================================================
    // ===== GET /desglose =====
    // =========================================================================================

    public DesgloseResponse desglose(LocalDate desde, LocalDate hasta) {
        validarPeriodo(desde, hasta);
        LocalDate hastaExclusivo = hasta.plusDays(1);
        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime finExclusivo = hastaExclusivo.atStartOfDay();

        // Tipo de habitación: importe de habitación y noches vendidas de cada tipo
        List<FilaDesgloseResponse> porTipo = filasConPorcentaje(
                reservaRepository.desglosePorTipoHabitacion(desde, hastaExclusivo), 1, 3, 2);

        // Servicio: importe y unidades vendidas
        List<FilaDesgloseResponse> porServicio = filasConPorcentaje(
                itemConsumoRepository.desglosePorServicio(inicio, finExclusivo), 0, 1, 2);

        // Método de pago: importe cobrado y número de pagos
        List<FilaDesgloseResponse> porMetodo = filasConPorcentaje(
                pagoRepository.desglosePorMetodoPago(inicio, finExclusivo), 0, 1, 2);

        return new DesgloseResponse(porTipo, porServicio, porMetodo,
                desempenoDeOperadores(inicio, finExclusivo));
    }

    // ===== Desempeño por operador =====
    // Mide ACTIVIDAD REGISTRADA, no ingresos del hotel: suma lo que cada operador cargó a las
    // cuentas más lo que cobró. Esos dos importes no se pueden sumar como facturación (uno es
    // consumo y el otro es caja del mismo consumo), pero sí describen cuánto movió cada persona,
    // que es justo lo que el administrador quiere comparar entre su equipo.
    private List<FilaDesgloseResponse> desempenoDeOperadores(LocalDateTime inicio, LocalDateTime finExclusivo) {
        Map<String, BigDecimal> importes = new LinkedHashMap<>();
        Map<String, Long> operaciones = new LinkedHashMap<>();

        for (Object[] fila : itemConsumoRepository.consumosPorOperador(inicio, finExclusivo)) {
            String nombre = (String) fila[0];
            importes.merge(nombre, comoBigDecimal(fila[1]), BigDecimal::add);
            operaciones.merge(nombre, comoLong(fila[2]), Long::sum);
        }
        for (Object[] fila : pagoRepository.pagosPorOperador(inicio, finExclusivo)) {
            String nombre = (String) fila[0];
            importes.merge(nombre, comoBigDecimal(fila[1]), BigDecimal::add);
            operaciones.merge(nombre, comoLong(fila[2]), Long::sum);
        }

        BigDecimal total = importes.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        List<FilaDesgloseResponse> filas = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entrada : importes.entrySet()) {
            filas.add(new FilaDesgloseResponse(
                    entrada.getKey(), entrada.getValue(),
                    operaciones.getOrDefault(entrada.getKey(), 0L),
                    porcentaje(entrada.getValue(), total)));
        }
        filas.sort(Comparator.comparing(FilaDesgloseResponse::valor).reversed());
        return filas;
    }

    // Convierte filas agregadas en filas de desglose, añadiendo el peso de cada una sobre el total
    // y ordenando de mayor a menor: las barras horizontales del panel se pintan en ese orden.
    private List<FilaDesgloseResponse> filasConPorcentaje(List<Object[]> filas,
                                                          int indiceEtiqueta,
                                                          int indiceValor,
                                                          int indiceCantidad) {
        BigDecimal total = BigDecimal.ZERO;
        for (Object[] fila : filas) {
            total = total.add(comoBigDecimal(fila[indiceValor]));
        }

        List<FilaDesgloseResponse> resultado = new ArrayList<>();
        for (Object[] fila : filas) {
            BigDecimal valor = comoBigDecimal(fila[indiceValor]);
            resultado.add(new FilaDesgloseResponse(
                    String.valueOf(fila[indiceEtiqueta]), valor,
                    comoLong(fila[indiceCantidad]), porcentaje(valor, total)));
        }
        resultado.sort(Comparator.comparing(FilaDesgloseResponse::valor).reversed());
        return resultado;
    }

    // =========================================================================================
    // ===== GET /reputacion =====
    // =========================================================================================

    public ReputacionResponse reputacion(LocalDate desde, LocalDate hasta) {
        validarPeriodo(desde, hasta);
        LocalDate hastaExclusivo = hasta.plusDays(1);

        // RN-14, global: el rating que de verdad tiene el hotel, no el del período
        Object[] global = primeraFila(resenaRepository.resumenGlobal());
        long total = comoLong(global[1]);
        BigDecimal promedio = total == 0 ? null : redondear(comoBigDecimal(global[0]));

        // Las cinco estrellas siempre presentes: una barra ausente se lee como dato perdido
        Map<Integer, Long> distribucion = new LinkedHashMap<>();
        for (int estrellas = 1; estrellas <= 5; estrellas++) {
            distribucion.put(estrellas, 0L);
        }
        for (Object[] fila : resenaRepository.distribucionGlobal()) {
            distribucion.put(comoEntero(fila[0]), comoLong(fila[1]));
        }

        List<RatingTipoResponse> porTipo = new ArrayList<>();
        for (Object[] fila : resenaRepository.ratingPorTipoConNombre()) {
            porTipo.add(new RatingTipoResponse(
                    comoLong(fila[0]), (String) fila[1],
                    redondear(comoBigDecimal(fila[2])), comoLong(fila[3])));
        }
        porTipo.sort(Comparator.comparing(RatingTipoResponse::promedio).reversed());

        List<ResenaRecienteResponse> recientes = new ArrayList<>();
        for (Resena resena : resenaRepository.buscarRecientes(PageRequest.of(0, MAX_RESENAS_RECIENTES))) {
            recientes.add(new ResenaRecienteResponse(
                    resena.getId(), resena.getCalificacion(), resena.getTitulo(), resena.getComentario(),
                    resena.getCliente().getNombreCompleto(),
                    resena.getTipoHabitacion().getNombre(),
                    resena.getFechaCreacion()));
        }

        // Nuevos y recurrentes del período: se consulta una vez y los nuevos salen por resta
        long atendidas = comoLong(primeraFila(reservaRepository.promediosDeReserva(desde, hastaExclusivo))[2]);
        long recurrentes = reservaRepository.contarReservasDeClientesRecurrentes(
                ESTADOS_VENDIBLES, desde, hastaExclusivo);
        long nuevos = Math.max(0, atendidas - recurrentes);

        BigDecimal leadTime = redondear(comoBigDecimal(
                primeraFila(reservaRepository.promediosDeReserva(desde, hastaExclusivo))[0]));

        return new ReputacionResponse(promedio, total, distribucion, porTipo, recientes,
                nuevos, recurrentes, leadTime);
    }

    // =========================================================================================
    // ===== GET /alertas =====
    // Ordenadas por severidad: lo que cuesta dinero hoy va antes que lo que puede esperar.
    // =========================================================================================

    public List<AlertaResponse> alertas() {
        LocalDate hoy = LocalDate.now();
        List<AlertaResponse> alertas = new ArrayList<>();

        // 1. Se van hoy y deben dinero: si se marchan, el cobro se pierde
        List<SaldoPendienteResponse> conSaldo = new ArrayList<>();
        for (Object[] fila : cuentaRepository.saldosDeCuentasConSalidaEn(EstadoCuenta.ABIERTA, hoy)) {
            if (comoBigDecimal(fila[4]).signum() > 0) {
                conSaldo.add(new SaldoPendienteResponse(comoLong(fila[0]), (String) fila[1],
                        ((String) fila[2]) + " " + ((String) fila[3]), comoBigDecimal(fila[4])));
            }
        }
        if (!conSaldo.isEmpty()) {
            BigDecimal totalDebido = conSaldo.stream()
                    .map(SaldoPendienteResponse::saldo)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            alertas.add(new AlertaResponse("SALIDA_CON_SALDO", SEVERIDAD_ALTA,
                    "Guests checking out today still owe money",
                    conSaldo.size() + " open account(s) with an outstanding balance of "
                            + totalDebido.setScale(ESCALA, RoundingMode.HALF_UP)
                            + ". Settle them before the guests leave.",
                    conSaldo.size(), "/admin/reservas", "Review stays"));
        }

        // 2. Llegan en 48 h y nadie las ha confirmado: la habitación no está garantizada
        List<Reserva> porConfirmar = reservaRepository.buscarPorConfirmarEntre(
                EstadoReserva.PENDIENTE, hoy, hoy.plusDays(HORAS_PARA_CONFIRMAR / 24));
        if (!porConfirmar.isEmpty()) {
            alertas.add(new AlertaResponse("PENDIENTE_POR_CONFIRMAR", SEVERIDAD_ALTA,
                    "Unconfirmed reservations arriving within 48 hours",
                    porConfirmar.size() + " reservation(s) are still pending. Confirm or release them "
                            + "so the rooms do not sit blocked.",
                    porConfirmar.size(), "/admin/reservas", "Confirm reservations"));
        }

        // 3. Habitaciones fuera de servicio justo cuando hacen falta
        long mantenimiento = habitacionRepository.countByEstado(EstadoHabitacion.MANTENIMIENTO);
        if (mantenimiento > 0) {
            BigDecimal ocupacionProxima = ocupacionMediaEntre(hoy, hoy.plusDays(6));
            if (ocupacionProxima.compareTo(OCUPACION_ALTA) > 0) {
                alertas.add(new AlertaResponse("MANTENIMIENTO_EN_PICO", SEVERIDAD_ALTA,
                        "Rooms under maintenance during a near-full week",
                        mantenimiento + " room(s) are out of service while occupancy for the next 7 days "
                                + "is " + ocupacionProxima + "%. Each one is a sale you cannot make.",
                        mantenimiento, "/admin/habitaciones", "See rooms"));
            }
        }

        // 4. Reseñas malas recientes: una queja se responde en días, no en meses
        List<Resena> negativas = resenaRepository.buscarNegativasDesde(
                CALIFICACION_NEGATIVA_MAXIMA, hoy.minusDays(DIAS_RESENAS_RECIENTES).atStartOfDay());
        if (!negativas.isEmpty()) {
            alertas.add(new AlertaResponse("RESENA_NEGATIVA", SEVERIDAD_MEDIA,
                    "Low ratings in the last 7 days",
                    negativas.size() + " review(s) of 1 or 2 stars. Read them before the pattern sets in.",
                    negativas.size(), "/admin/resenas", "Read reviews"));
        }

        // 5. Caídas frente al período anterior: el panel avisa de la tendencia, no solo del nivel
        alertas.addAll(alertasDeCaida(hoy));

        // 6. Planes de visita que la validación marcó como dudosos en estadías próximas
        List<PlanVisita> revisables = planVisitaRepository.buscarQueRequierenRevision(
                hoy, hoy.plusDays(DIAS_PLANES_PROXIMOS), ESTADOS_VENDIBLES);
        if (!revisables.isEmpty()) {
            alertas.add(new AlertaResponse("PLAN_REQUIERE_REVISION", SEVERIDAD_BAJA,
                    "Visit plans flagged for review",
                    revisables.size() + " upcoming stay(s) have an AI plan that could not be fully verified.",
                    revisables.size(), "/admin/reservas", "Check plans"));
        }

        alertas.sort(Comparator.comparingInt(a -> ordenDeSeveridad(a.severidad())));
        return alertas;
    }

    // Compara los últimos 30 días con los 30 anteriores: el mismo criterio del resumen por defecto
    private List<AlertaResponse> alertasDeCaida(LocalDate hoy) {
        List<AlertaResponse> alertas = new ArrayList<>();
        LocalDate hasta = hoy;
        LocalDate desde = hoy.minusDays(DIAS_PERIODO_POR_DEFECTO - 1L);
        LocalDate hastaAnterior = desde.minusDays(1);
        LocalDate desdeAnterior = hastaAnterior.minusDays(DIAS_PERIODO_POR_DEFECTO - 1L);

        KpisResponse actual = calcularKpis(desde, hasta);
        KpisResponse anterior = calcularKpis(desdeAnterior, hastaAnterior);

        BigDecimal caidaOcupacion = caidaPorcentual(anterior.ocupacionPorcentaje(), actual.ocupacionPorcentaje());
        if (caidaOcupacion.compareTo(CAIDA_RELEVANTE) > 0) {
            alertas.add(new AlertaResponse("CAIDA_OCUPACION", SEVERIDAD_MEDIA,
                    "Occupancy is down " + caidaOcupacion + "% versus the previous 30 days",
                    "It went from " + anterior.ocupacionPorcentaje() + "% to " + actual.ocupacionPorcentaje()
                            + "%. Look at rates and at which room types lost nights.",
                    0, "/admin/dashboard", "See the breakdown"));
        }

        BigDecimal caidaIngresos = caidaPorcentual(anterior.ingresosTotales(), actual.ingresosTotales());
        if (caidaIngresos.compareTo(CAIDA_RELEVANTE) > 0) {
            alertas.add(new AlertaResponse("CAIDA_INGRESOS", SEVERIDAD_MEDIA,
                    "Revenue is down " + caidaIngresos + "% versus the previous 30 days",
                    "Billed revenue fell from " + anterior.ingresosTotales() + " to " + actual.ingresosTotales()
                            + ". Check whether it is rooms or services.",
                    0, "/admin/dashboard", "See the breakdown"));
        }
        return alertas;
    }

    // Ocupación media de una ventana corta, reutilizando la serie diaria ya agregada
    private BigDecimal ocupacionMediaEntre(LocalDate desde, LocalDate hasta) {
        long inventario = habitacionRepository.contarInventario(EstadoHabitacion.DESHABILITADA);
        if (inventario == 0) {
            return BigDecimal.ZERO;
        }
        long noches = 0;
        int dias = 0;
        for (Object[] fila : reservaRepository.serieDiariaHabitaciones(desde, rangoSystemRange(desde, hasta))) {
            noches += comoLong(fila[1]);
            dias++;
        }
        return porcentaje(BigDecimal.valueOf(noches),
                BigDecimal.valueOf(inventario).multiply(BigDecimal.valueOf(dias)));
    }

    // Cuánto bajó, en porcentaje, de un período al siguiente. 0 si subió o si no había base.
    private static BigDecimal caidaPorcentual(BigDecimal anterior, BigDecimal actual) {
        if (anterior == null || anterior.signum() <= 0 || actual == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal diferencia = anterior.subtract(actual);
        if (diferencia.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return porcentaje(diferencia, anterior);
    }

    private static int ordenDeSeveridad(String severidad) {
        return switch (severidad) {
            case SEVERIDAD_ALTA -> 0;
            case SEVERIDAD_MEDIA -> 1;
            default -> 2;
        };
    }

    // =========================================================================================
    // ===== Período y utilidades =====
    // =========================================================================================

    // El panel puede llamar sin fechas: por defecto, los últimos 30 días contando hoy
    public LocalDate desdePorDefecto() {
        return LocalDate.now().minusDays(DIAS_PERIODO_POR_DEFECTO - 1L);
    }

    public LocalDate hastaPorDefecto() {
        return LocalDate.now();
    }

    private void validarPeriodo(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException("The period needs both the desde and hasta dates.");
        }
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("The desde date cannot be later than hasta.");
        }
        if (diasDe(desde, hasta) > MAX_DIAS_PERIODO) {
            throw new IllegalArgumentException("The period cannot be longer than " + MAX_DIAS_PERIODO + " days.");
        }
    }

    private static int diasDe(LocalDate desde, LocalDate hasta) {
        return (int) ChronoUnit.DAYS.between(desde, hasta) + 1;
    }

    // SYSTEM_RANGE(0, n) genera n + 1 filas: para cubrir el período se le pasa un día menos
    private static int rangoSystemRange(LocalDate desde, LocalDate hasta) {
        return diasDe(desde, hasta) - 1;
    }

    private static LocalDateTime finDeDia(LocalDate fecha) {
        return fecha.atTime(LocalTime.MAX);
    }

    private static LocalDate claveDeAgrupacion(LocalDate dia, String modo) {
        return switch (modo) {
            // La semana se identifica por su lunes, para que la etiqueta sea una fecha real
            case "semana" -> dia.minusDays(dia.getDayOfWeek().getValue() - 1L);
            case "mes" -> YearMonth.from(dia).atDay(1);
            default -> dia;
        };
    }

    private static String etiquetaDe(LocalDate fecha, String modo) {
        return switch (modo) {
            case "semana" -> fecha.format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH));
            case "mes" -> fecha.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH));
            default -> fecha.format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH));
        };
    }

    private static Map<LocalDate, BigDecimal> porDia(List<Object[]> filas) {
        Map<LocalDate, BigDecimal> mapa = new LinkedHashMap<>();
        for (Object[] fila : filas) {
            mapa.put(comoLocalDate(fila[0]), comoBigDecimal(fila[1]));
        }
        return mapa;
    }

    // Las consultas de una sola fila devuelven lista: si la tabla está vacía, ceros en lugar de error
    private static Object[] primeraFila(List<Object[]> filas) {
        return filas.isEmpty() ? new Object[]{null, null, null, null, null} : filas.get(0);
    }

    // ===== Conversión de las filas agregadas =====
    // Las consultas nativas devuelven los tipos que da JDBC (Integer, Long, BigDecimal, Double,
    // java.sql.Date), y los de JPQL no siempre coinciden. Se normaliza en un solo sitio.

    private static long comoLong(Object valor) {
        return valor == null ? 0L : ((Number) valor).longValue();
    }

    private static int comoEntero(Object valor) {
        return valor == null ? 0 : ((Number) valor).intValue();
    }

    private static BigDecimal comoBigDecimal(Object valor) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        if (valor instanceof BigDecimal decimal) {
            return decimal;
        }
        return BigDecimal.valueOf(((Number) valor).doubleValue());
    }

    private static LocalDate comoLocalDate(Object valor) {
        if (valor instanceof LocalDate fecha) {
            return fecha;
        }
        if (valor instanceof java.sql.Date fecha) {
            return fecha.toLocalDate();
        }
        if (valor instanceof java.sql.Timestamp instante) {
            return instante.toLocalDateTime().toLocalDate();
        }
        throw new IllegalStateException("Unexpected date type in the series: " + valor);
    }

    private static BigDecimal dividir(BigDecimal numerador, BigDecimal denominador) {
        if (denominador == null || denominador.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return numerador.divide(denominador, ESCALA, RoundingMode.HALF_UP);
    }

    private static BigDecimal porcentaje(BigDecimal parte, BigDecimal total) {
        if (total == null || total.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return parte.multiply(CIEN).divide(total, ESCALA, RoundingMode.HALF_UP);
    }

    private static BigDecimal redondear(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor.setScale(ESCALA, RoundingMode.HALF_UP);
    }
}
