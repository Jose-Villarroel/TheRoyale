package com.theroyale.backend.service.plan;

import com.theroyale.backend.dto.plan.BloquePlanResponse;
import com.theroyale.backend.dto.plan.ClimaDiaResponse;
import com.theroyale.backend.dto.plan.DiaPlanResponse;
import com.theroyale.backend.dto.plan.PreferenciasPlan;
import com.theroyale.backend.errors.PlanNoConfiableException;
import com.theroyale.backend.model.FuentePlan;
import com.theroyale.backend.model.RestauranteHotel;
import com.theroyale.backend.model.TipoBloque;
import com.theroyale.backend.service.plan.ContextoPlan.EventoContexto;
import com.theroyale.backend.service.plan.ContextoPlan.LugarContexto;
import com.theroyale.backend.service.plan.ContextoPlan.ServicioContexto;
import com.theroyale.backend.service.plan.PlanGenerado.BloqueGenerado;
import com.theroyale.backend.service.plan.PlanGenerado.DiaGenerado;
import com.theroyale.backend.service.plan.fuentes.ClimaDia;
import com.theroyale.backend.service.plan.fuentes.Distancias;
import com.theroyale.backend.service.plan.fuentes.EventoCandidato;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;

// ===== Valida cada bloque que propuso la IA contra el contexto de ESA generación (sección 8) =====
// Reglas: LUGAR en la lista enviada; EVENTO existente, ese día y dentro de su horario; SERVICIO_HOTEL
// con servicioId real y la característica exacta; RESTAURANTE_HOTEL abierto a esa hora; horas HH:mm
// con inicio < fin y sin cruces; llegada desde las 15:00 y salida hasta las 12:00; y (criterio 4) sin
// lugares al aire libre en días con 60 % o más de lluvia. Faltan días: TIEMPO_LIBRE; sobran: se quitan.
// Después ENRIQUECE: nombre oficial, URL, traslado, bajo techo y precio salen de la fuente; de la IA
// solo queda el motivo. Si se elimina más del 40 % de los bloques se reintenta una vez y luego 502.
@Service
public class ValidadorPlanService {

    static final double MAXIMA_PROPORCION_ELIMINADA = 0.40;
    static final String NOTA_CHECK_IN = "Check-in from 15:00. Your suite will be ready when you arrive.";
    static final String NOTA_DIA_LIBRE = "This day is yours to explore at your own pace.";
    static final String FUENTE_LUGARES = "The Royale curated places";
    static final String FUENTE_HOTEL = "The Royale";
    static final String EN_EL_HOTEL = "In the hotel";
    static final int LLUVIA_SOLO_BAJO_TECHO = 60;

    private static final Pattern HORA = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");
    private static final Set<String> SERVICIOS_DENTRO_DEL_HOTEL = Set.of("wellness", "dining", "business");
    private static final Logger log = LoggerFactory.getLogger(ValidadorPlanService.class);

    // ===== Resultado de validar una respuesta de la IA =====
    public record Resultado(String resumen, List<DiaPlanResponse> dias, int bloquesRecibidos, int bloquesEliminados,
                            String modelo) {

        public double proporcionEliminada() {
            return bloquesRecibidos == 0 ? 1.0 : (double) bloquesEliminados / bloquesRecibidos;
        }
    }

    // ===== Genera y valida; si se elimina más del 40 % vuelve a generar una vez, y si no, 502 =====
    public Resultado generarValidado(Supplier<PlanGenerado.Respuesta> generar, ContextoPlan contexto,
                                     DatosEstadia estadia, List<LocalDate> dias, PreferenciasPlan preferencias) {
        for (int intento = 1; intento <= 2; intento++) {
            PlanGenerado.Respuesta respuesta = generar.get();
            Resultado resultado = validar(respuesta.plan(), contexto, estadia, dias, preferencias, respuesta.modelo());
            if (resultado.proporcionEliminada() <= MAXIMA_PROPORCION_ELIMINADA) {
                return resultado;
            }
            log.warn("Plan descartado (intento {}): se eliminaron {} de {} bloques", intento,
                    resultado.bloquesEliminados(), resultado.bloquesRecibidos());
        }
        throw new PlanNoConfiableException();
    }

    // ===== Reglas de la sección 8 y enriquecimiento =====
    public Resultado validar(PlanGenerado plan, ContextoPlan contexto, DatosEstadia estadia,
                             List<LocalDate> dias, PreferenciasPlan preferencias, String modelo) {
        boolean movilidadReducida = preferencias != null && preferencias.movilidadReducida();

        // Un día por fecha de la estadía: las fechas que sobran o se repiten se descartan
        Map<LocalDate, DiaGenerado> porFecha = new LinkedHashMap<>();
        if (plan != null && plan.dias() != null) {
            for (DiaGenerado dia : plan.dias()) {
                LocalDate fecha = fecha(dia == null ? null : dia.fecha());
                if (fecha != null && dias.contains(fecha)) {
                    porFecha.putIfAbsent(fecha, dia);
                }
            }
        }

        int recibidos = 0;
        int eliminados = 0;
        List<DiaPlanResponse> resultado = new ArrayList<>();
        for (LocalDate fecha : dias) {
            ClimaDia clima = contexto.clima().get(fecha);
            String tipoDia = estadia.tipoDia(fecha);
            DiaGenerado generado = porFecha.get(fecha);

            List<BloquePlanResponse> bloques = new ArrayList<>();
            String titulo;
            if (generado == null) {
                // Falta el día: se completa con tiempo libre
                titulo = "A free day";
                bloques.add(tiempoLibre(diaLibreInicio(tipoDia), diaLibreFin(tipoDia), "", NOTA_DIA_LIBRE));
            } else {
                titulo = generado.titulo() == null || generado.titulo().isBlank() ? "Your day" : generado.titulo().trim();
                List<BloqueGenerado> propuestos = generado.bloques() == null ? List.of() : generado.bloques();
                recibidos += propuestos.size();

                List<BloqueValido> validos = new ArrayList<>();
                for (BloqueGenerado bloque : propuestos) {
                    Optional<BloqueValido> valido = validarBloque(bloque, fecha, tipoDia, clima, contexto, movilidadReducida);
                    if (valido.isPresent()) {
                        validos.add(valido.get());
                    } else {
                        eliminados++;
                    }
                }

                // Sin cruces: se recorre por hora de inicio y se elimina el que se cruza con el anterior
                validos.sort(Comparator.comparing(BloqueValido::inicio));
                LocalTime finAnterior = null;
                for (BloqueValido valido : validos) {
                    if (finAnterior != null && valido.inicio().isBefore(finAnterior)) {
                        eliminados++;
                        continue;
                    }
                    bloques.add(valido.respuesta());
                    finAnterior = valido.fin();
                }
                if (bloques.isEmpty()) {
                    bloques.add(tiempoLibre(diaLibreInicio(tipoDia), diaLibreFin(tipoDia), "", NOTA_DIA_LIBRE));
                }
            }

            if (DiaPlanResponse.LLEGADA.equals(tipoDia)) {
                bloques.add(0, notaCheckIn(bloques));
            }
            resultado.add(new DiaPlanResponse(fecha, estadia.numero(fecha), tipoDia, titulo, climaRespuesta(clima),
                    List.copyOf(bloques)));
        }

        String resumen = plan == null || plan.resumen() == null ? "" : plan.resumen().trim();
        return new Resultado(resumen, resultado, recibidos, eliminados, modelo);
    }

    private record BloqueValido(LocalTime inicio, LocalTime fin, BloquePlanResponse respuesta) {
    }

    private Optional<BloqueValido> validarBloque(BloqueGenerado bloque, LocalDate fecha, String tipoDia, ClimaDia clima,
                                                 ContextoPlan contexto, boolean movilidadReducida) {
        if (bloque == null || bloque.inicio() == null || bloque.fin() == null
                || !HORA.matcher(bloque.inicio().trim()).matches() || !HORA.matcher(bloque.fin().trim()).matches()) {
            return Optional.empty();
        }
        LocalTime inicio = LocalTime.parse(bloque.inicio().trim());
        LocalTime fin = LocalTime.parse(bloque.fin().trim());
        if (!inicio.isBefore(fin)) {
            return Optional.empty();
        }
        if (DiaPlanResponse.LLEGADA.equals(tipoDia) && inicio.isBefore(DatosEstadia.CHECK_IN)) {
            return Optional.empty();
        }
        if (DiaPlanResponse.SALIDA.equals(tipoDia) && fin.isAfter(DatosEstadia.CHECK_OUT)) {
            return Optional.empty();
        }

        TipoBloque tipo;
        try {
            tipo = TipoBloque.valueOf(bloque.tipo() == null ? "" : bloque.tipo().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
        String referencia = bloque.referenciaId() == null ? "" : bloque.referenciaId().trim();
        String motivo = bloque.motivo() == null ? "" : bloque.motivo().trim();
        String id = UUID.randomUUID().toString();
        String hInicio = inicio.toString();
        String hFin = fin.toString();

        return switch (tipo) {
            case LUGAR -> contexto.lugar(referencia)
                    .filter(lugar -> lugar.bajoTecho() || clima == null || clima.probLluviaOCero() < LLUVIA_SOLO_BAJO_TECHO)
                    .map(lugar -> new BloqueValido(inicio, fin, enriquecerLugar(id, hInicio, hFin, motivo, lugar, movilidadReducida)));
            case EVENTO -> contexto.evento(referencia)
                    .filter(evento -> dentroDelEvento(evento.evento(), fecha, inicio, fin))
                    .filter(evento -> evento.evento().bajoTecho() || clima == null
                            || clima.probLluviaOCero() < LLUVIA_SOLO_BAJO_TECHO)
                    .map(evento -> new BloqueValido(inicio, fin, enriquecerEvento(id, hInicio, hFin, motivo, evento, movilidadReducida)));
            case SERVICIO_HOTEL -> contexto.servicio(referencia)
                    .filter(servicio -> bloque.caracteristica() != null
                            && servicio.caracteristicas().contains(bloque.caracteristica()))
                    .map(servicio -> new BloqueValido(inicio, fin,
                            enriquecerServicio(id, hInicio, hFin, motivo, servicio, bloque.caracteristica())));
            case RESTAURANTE_HOTEL -> contexto.restaurante(referencia)
                    .filter(r -> HorariosRestaurantes.abierto(r.restaurante(), fecha, inicio, fin))
                    .map(r -> new BloqueValido(inicio, fin, enriquecerRestaurante(id, hInicio, hFin, motivo, r.restaurante())));
            case TIEMPO_LIBRE -> Optional.of(new BloqueValido(inicio, fin,
                    tiempoLibre(hInicio, hFin, "", motivo.isEmpty() ? NOTA_DIA_LIBRE : motivo)));
        };
    }

    // El bloque cae dentro del horario del evento ese día (si el evento dura varios días, ese día completo cuenta)
    private boolean dentroDelEvento(EventoCandidato evento, LocalDate fecha, LocalTime inicio, LocalTime fin) {
        if (!evento.ocurreEl(fecha)) {
            return false;
        }
        LocalDateTime desde = fecha.atTime(inicio);
        LocalDateTime hasta = fecha.atTime(fin);
        return !desde.isBefore(evento.inicio()) && !hasta.isAfter(evento.fin());
    }

    // ===== Enriquecimiento: los datos visibles salen de la fuente, no de la IA =====
    private BloquePlanResponse enriquecerLugar(String id, String inicio, String fin, String motivo, LugarContexto lugar,
                                               boolean movilidadReducida) {
        return new BloquePlanResponse(id, inicio, fin, TipoBloque.LUGAR, lugar.categoria().name(), lugar.nombre(), motivo,
                Distancias.traslado(lugar.distanciaMetros(), movilidadReducida), lugar.bajoTecho(), null, lugar.url(),
                FUENTE_LUGARES, null, null, null);
    }

    private BloquePlanResponse enriquecerEvento(String id, String inicio, String fin, String motivo, EventoContexto contexto,
                                                boolean movilidadReducida) {
        EventoCandidato evento = contexto.evento();
        String traslado = contexto.distanciaMetros() == null ? null
                : Distancias.traslado(contexto.distanciaMetros(), movilidadReducida);
        String fuente = evento.fuente() == FuentePlan.TICKETMASTER ? "Ticketmaster" : "NYC Open Data";
        return new BloquePlanResponse(id, inicio, fin, TipoBloque.EVENTO, evento.categoria(), evento.titulo(), motivo,
                traslado, evento.bajoTecho(), evento.lugar(), evento.url(), fuente, null, null, null);
    }

    // titulo = el nombre de la característica ("Signature Massage - 60 or 90 min..." -> "Signature Massage")
    private BloquePlanResponse enriquecerServicio(String id, String inicio, String fin, String motivo,
                                                  ServicioContexto servicio, String caracteristica) {
        String titulo = caracteristica.contains(" - ") ? caracteristica.substring(0, caracteristica.indexOf(" - ")) : caracteristica;
        String nombre = servicio.nombre() == null ? "" : servicio.nombre().toLowerCase(Locale.ROOT);
        String traslado = SERVICIOS_DENTRO_DEL_HOTEL.contains(nombre) ? EN_EL_HOTEL : null;
        String categoria = "wellness".equals(nombre) ? "SPA" : servicio.nombre().toUpperCase(Locale.ROOT);
        // agregable se fija al responder según el estado actual de la reserva (BloquePlanResponse.conAgregable)
        return new BloquePlanResponse(id, inicio, fin, TipoBloque.SERVICIO_HOTEL, categoria, titulo.trim(), motivo,
                traslado, true, null, null, FUENTE_HOTEL, servicio.id(), servicio.precio(), false);
    }

    private BloquePlanResponse enriquecerRestaurante(String id, String inicio, String fin, String motivo,
                                                     RestauranteHotel restaurante) {
        return new BloquePlanResponse(id, inicio, fin, TipoBloque.RESTAURANTE_HOTEL, HorariosRestaurantes.categoria(restaurante),
                HorariosRestaurantes.nombre(restaurante), motivo, EN_EL_HOTEL, true, null, null, FUENTE_HOTEL,
                null, null, null);
    }

    static BloquePlanResponse tiempoLibre(String inicio, String fin, String titulo, String nota) {
        return new BloquePlanResponse(UUID.randomUUID().toString(), inicio, fin, TipoBloque.TIEMPO_LIBRE, "FREE TIME",
                titulo, nota, null, null, null, null, null, null, null, null);
    }

    // La nota de check-in va primero; dura hasta las 15:30 o hasta que empiece el primer bloque
    private BloquePlanResponse notaCheckIn(List<BloquePlanResponse> bloques) {
        LocalTime fin = LocalTime.of(15, 30);
        if (!bloques.isEmpty()) {
            LocalTime primero = LocalTime.parse(bloques.get(0).inicio());
            if (primero.isBefore(fin)) {
                fin = primero;
            }
        }
        return tiempoLibre(DatosEstadia.CHECK_IN.toString(), fin.toString(), "", NOTA_CHECK_IN);
    }

    private static String diaLibreInicio(String tipoDia) {
        return DiaPlanResponse.LLEGADA.equals(tipoDia) ? "15:30" : DiaPlanResponse.SALIDA.equals(tipoDia) ? "09:00" : "10:00";
    }

    private static String diaLibreFin(String tipoDia) {
        return DiaPlanResponse.LLEGADA.equals(tipoDia) ? "20:00" : DiaPlanResponse.SALIDA.equals(tipoDia) ? "11:30" : "18:00";
    }

    static ClimaDiaResponse climaRespuesta(ClimaDia clima) {
        return clima == null ? null
                : new ClimaDiaResponse(clima.resumen(), clima.maxC(), clima.minC(), clima.probLluvia(), clima.tipo());
    }

    private static LocalDate fecha(String texto) {
        try {
            return texto == null ? null : LocalDate.parse(texto.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
