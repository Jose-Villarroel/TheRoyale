package com.theroyale.backend.service.plan;

import com.theroyale.backend.dto.plan.BloquePlanResponse;
import com.theroyale.backend.dto.plan.DiaPlanResponse;
import com.theroyale.backend.dto.plan.PreferenciasPlan;
import com.theroyale.backend.errors.PlanNoConfiableException;
import com.theroyale.backend.model.CategoriaLugar;
import com.theroyale.backend.model.FuentePlan;
import com.theroyale.backend.model.Interes;
import com.theroyale.backend.model.Presupuesto;
import com.theroyale.backend.model.RestauranteHotel;
import com.theroyale.backend.model.Ritmo;
import com.theroyale.backend.model.TipoBloque;
import com.theroyale.backend.model.TipoClima;
import com.theroyale.backend.service.plan.ContextoPlan.EventoContexto;
import com.theroyale.backend.service.plan.ContextoPlan.LugarContexto;
import com.theroyale.backend.service.plan.ContextoPlan.RestauranteContexto;
import com.theroyale.backend.service.plan.ContextoPlan.ServicioContexto;
import com.theroyale.backend.service.plan.PlanGenerado.BloqueGenerado;
import com.theroyale.backend.service.plan.PlanGenerado.DiaGenerado;
import com.theroyale.backend.service.plan.fuentes.ClimaDia;
import com.theroyale.backend.service.plan.fuentes.EventoCandidato;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// ===== Pruebas de ValidadorPlanService: cada regla de la sección 8, el enriquecimiento y el reintento =====
// Sin Spring ni Mockito: el contexto se arma a mano. Estadía del sábado 12 al martes 15 de diciembre de 2026.
class ValidadorPlanServiceTest {

    private static final LocalDate LLEGADA = LocalDate.of(2026, 12, 12);
    private static final LocalDate COMPLETO = LocalDate.of(2026, 12, 13);
    private static final LocalDate LLUVIOSO = LocalDate.of(2026, 12, 14);
    private static final LocalDate SALIDA = LocalDate.of(2026, 12, 15);
    private static final List<LocalDate> DIAS = List.of(LLEGADA, COMPLETO, LLUVIOSO, SALIDA);
    private static final DatosEstadia ESTADIA = new DatosEstadia(LLEGADA, SALIDA, 2, "Luxury");
    private static final String MASAJE = "Signature Massage - 60 or 90 min, Swedish, deep tissue and hot stone";

    private final ValidadorPlanService validador = new ValidadorPlanService();
    private final ContextoPlan contexto = contexto();

    // ===== LUGAR =====
    @Test
    void validadorPlan_validar_lugarDeLaLista_seEnriqueceConLosDatosDelCatalogo() {
        // Arrange: la IA escribe otro título; vale solo su motivo
        PlanGenerado plan = plan(dia(COMPLETO, bloque("10:00", "12:30", "LUGAR", "1", null, "MoMA!!", "Art first.")));

        // Act
        BloquePlanResponse bloque = soloBloque(validar(plan), COMPLETO);

        // Assert
        assertThat(bloque.titulo()).isEqualTo("The Museum of Modern Art (MoMA)");
        assertThat(bloque.motivo()).isEqualTo("Art first.");
        assertThat(bloque.url()).isEqualTo("https://www.moma.org");
        assertThat(bloque.traslado()).isEqualTo("4 min walk");
        assertThat(bloque.bajoTecho()).isTrue();
        assertThat(bloque.categoria()).isEqualTo("MUSEO");
        assertThat(bloque.fuente()).isEqualTo("The Royale curated places");
        assertThat(bloque.servicioId()).isNull();
        assertThat(bloque.id()).hasSize(36);
    }

    @Test
    void validadorPlan_validar_lugarQueNoEstaEnLaLista_seElimina() {
        // Arrange
        PlanGenerado plan = plan(dia(COMPLETO,
                bloque("10:00", "12:00", "LUGAR", "99", null, "Invented Museum", "Made up."),
                bloque("13:00", "15:00", "LUGAR", "1", null, "MoMA", "Real.")));

        // Act
        ValidadorPlanService.Resultado resultado = validar(plan);

        // Assert
        assertThat(bloques(resultado, COMPLETO)).extracting(BloquePlanResponse::titulo)
                .containsExactly("The Museum of Modern Art (MoMA)");
        assertThat(resultado.bloquesEliminados()).isEqualTo(1);
    }

    @Test
    void validadorPlan_validar_lugarAlAireLibreEnDiaDeLluvia_seElimina() {
        // Arrange: el 14 tiene 70 % de lluvia (criterio de aceptación 4)
        PlanGenerado plan = plan(dia(LLUVIOSO,
                bloque("10:00", "12:00", "LUGAR", "2", null, "Central Park", "A walk."),
                bloque("13:00", "15:00", "LUGAR", "1", null, "MoMA", "Indoors.")));

        // Act
        List<BloquePlanResponse> bloques = bloques(validar(plan), LLUVIOSO);

        // Assert
        assertThat(bloques).extracting(BloquePlanResponse::titulo).containsExactly("The Museum of Modern Art (MoMA)");
        assertThat(bloques).noneMatch(b -> b.tipo() == TipoBloque.LUGAR && Boolean.FALSE.equals(b.bajoTecho()));
    }

    @Test
    void validadorPlan_validar_conMovilidadReducida_elTrasladoEsEnAuto() {
        // Arrange
        PreferenciasPlan movilidad = new PreferenciasPlan(List.of(Interes.MUSEOS), Ritmo.TRANQUILO, Presupuesto.ALTO,
                List.of(), true, false, null);
        PlanGenerado plan = plan(dia(COMPLETO, bloque("10:00", "12:00", "LUGAR", "2", null, "Central Park", "Calm.")));

        // Act
        BloquePlanResponse bloque = soloBloque(validador.validar(plan, contexto, ESTADIA, DIAS, movilidad, "m"), COMPLETO);

        // Assert
        assertThat(bloque.traslado()).endsWith("min by car");
    }

    // ===== EVENTO =====
    @Test
    void validadorPlan_validar_eventoValidoDentroDeSuHorario_seEnriqueceConLaFuente() {
        // Arrange
        PlanGenerado plan = plan(dia(COMPLETO, bloque("19:30", "22:00", "EVENTO", "tm-1", null, "x", "A concert.")));

        // Act
        BloquePlanResponse bloque = soloBloque(validar(plan), COMPLETO);

        // Assert
        assertThat(bloque.titulo()).isEqualTo("Holiday Concert");
        assertThat(bloque.lugar()).isEqualTo("Carnegie Hall");
        assertThat(bloque.url()).isEqualTo("https://www.ticketmaster.com/event/1");
        assertThat(bloque.fuente()).isEqualTo("Ticketmaster");
        assertThat(bloque.categoria()).isEqualTo("MUSICA");
        assertThat(bloque.traslado()).isEqualTo("10 min walk");
    }

    @Test
    void validadorPlan_validar_eventoConIdInexistente_seElimina() {
        // Arrange: criterio de aceptación 3
        PlanGenerado plan = plan(dia(COMPLETO,
                bloque("19:30", "22:00", "EVENTO", "tm-no-existe", null, "Fake show", "Made up."),
                bloque("10:00", "12:00", "LUGAR", "1", null, "MoMA", "Real.")));

        // Act
        ValidadorPlanService.Resultado resultado = validar(plan);

        // Assert
        assertThat(bloques(resultado, COMPLETO)).noneMatch(b -> b.tipo() == TipoBloque.EVENTO);
        assertThat(resultado.bloquesEliminados()).isEqualTo(1);
    }

    @Test
    void validadorPlan_validar_eventoEnOtroDiaOFueraDeSuHorario_seElimina() {
        // Arrange: tm-1 es el 13 de 19:30 a 22:30
        PlanGenerado plan = plan(
                dia(LLEGADA, bloque("19:30", "22:00", "EVENTO", "tm-1", null, "x", "Wrong day.")),
                dia(COMPLETO, bloque("18:00", "20:00", "EVENTO", "tm-1", null, "x", "Too early.")));

        // Act
        ValidadorPlanService.Resultado resultado = validar(plan);

        // Assert
        assertThat(resultado.bloquesEliminados()).isEqualTo(2);
        assertThat(bloques(resultado, COMPLETO)).noneMatch(b -> b.tipo() == TipoBloque.EVENTO);
    }

    // ===== SERVICIO_HOTEL =====
    @Test
    void validadorPlan_validar_servicioConCaracteristicaExacta_traePrecioYServicioId() {
        // Arrange
        PlanGenerado plan = plan(dia(COMPLETO, bloque("16:00", "17:30", "SERVICIO_HOTEL", "1", MASAJE, "Massage", "Rest.")));

        // Act
        BloquePlanResponse bloque = soloBloque(validar(plan), COMPLETO);

        // Assert
        assertThat(bloque.titulo()).isEqualTo("Signature Massage");
        assertThat(bloque.servicioId()).isEqualTo(1L);
        assertThat(bloque.precio()).isEqualTo(45.0);
        assertThat(bloque.categoria()).isEqualTo("SPA");
        assertThat(bloque.traslado()).isEqualTo("In the hotel");
        assertThat(bloque.fuente()).isEqualTo("The Royale");
    }

    @Test
    void validadorPlan_validar_servicioConCaracteristicaInexactaOServicioInexistente_seElimina() {
        // Arrange
        PlanGenerado plan = plan(dia(COMPLETO,
                bloque("10:00", "11:00", "SERVICIO_HOTEL", "1", "Signature Massage", "Massage", "Not exact."),
                bloque("12:00", "13:00", "SERVICIO_HOTEL", "77", MASAJE, "Massage", "No such service."),
                bloque("14:00", "15:00", "LUGAR", "1", null, "MoMA", "Real.")));

        // Act
        ValidadorPlanService.Resultado resultado = validar(plan);

        // Assert
        assertThat(resultado.bloquesEliminados()).isEqualTo(2);
        assertThat(bloques(resultado, COMPLETO)).noneMatch(b -> b.tipo() == TipoBloque.SERVICIO_HOTEL);
    }

    // ===== RESTAURANTE_HOTEL =====
    @Test
    void validadorPlan_validar_restauranteDentroYFueraDeSuHorario() {
        // Arrange: el Signature cierra entre las 15:00 y las 18:00
        PlanGenerado plan = plan(dia(COMPLETO,
                bloque("19:00", "21:00", "RESTAURANTE_HOTEL", "SIGNATURE_RESTAURANT", null, "Dinner", "Anniversary."),
                bloque("16:00", "17:00", "RESTAURANTE_HOTEL", "SIGNATURE_RESTAURANT", null, "Dinner", "Closed then."),
                bloque("12:00", "13:00", "RESTAURANTE_HOTEL", "NO_EXISTE", null, "Lunch", "Unknown.")));

        // Act
        ValidadorPlanService.Resultado resultado = validar(plan);

        // Assert
        List<BloquePlanResponse> bloques = bloques(resultado, COMPLETO);
        assertThat(bloques).hasSize(1);
        assertThat(bloques.get(0).titulo()).isEqualTo("Signature Restaurant");
        assertThat(bloques.get(0).traslado()).isEqualTo("In the hotel");
        assertThat(resultado.bloquesEliminados()).isEqualTo(2);
    }

    @Test
    void validadorPlan_validar_desayunoHastaLas11SoloElFinDeSemana() {
        // Arrange: el 13 es domingo y el 14 lunes
        PlanGenerado plan = plan(
                dia(COMPLETO, bloque("10:00", "10:50", "RESTAURANTE_HOTEL", "BREAKFAST_LOUNGE", null, "B", "Sunday.")),
                dia(LLUVIOSO, bloque("10:00", "10:50", "RESTAURANTE_HOTEL", "BREAKFAST_LOUNGE", null, "B", "Monday.")));

        // Act
        ValidadorPlanService.Resultado resultado = validar(plan);

        // Assert
        assertThat(bloques(resultado, COMPLETO)).extracting(BloquePlanResponse::tipo).containsExactly(TipoBloque.RESTAURANTE_HOTEL);
        assertThat(bloques(resultado, LLUVIOSO)).extracting(BloquePlanResponse::tipo).containsExactly(TipoBloque.TIEMPO_LIBRE);
    }

    // ===== Horas y cruces =====
    @Test
    void validadorPlan_validar_horasInvalidasOInicioNoAnteriorAFin_seEliminan() {
        // Arrange
        PlanGenerado plan = plan(dia(COMPLETO,
                bloque("9:00", "10:00", "LUGAR", "1", null, "x", "Bad format."),
                bloque("25:00", "26:00", "LUGAR", "1", null, "x", "Bad hour."),
                bloque("14:00", "14:00", "LUGAR", "1", null, "x", "Zero length."),
                bloque("16:00", "15:00", "LUGAR", "1", null, "x", "Backwards."),
                bloque("10:00", "12:00", "LUGAR", "1", null, "x", "Good.")));

        // Act
        ValidadorPlanService.Resultado resultado = validar(plan);

        // Assert
        assertThat(bloques(resultado, COMPLETO)).extracting(BloquePlanResponse::motivo).containsExactly("Good.");
        assertThat(resultado.bloquesEliminados()).isEqualTo(4);
    }

    @Test
    void validadorPlan_validar_bloqueQueSeCruza_seEliminaYLosDemasQuedanOrdenados() {
        // Arrange: llegan desordenados; el de 11:00 se cruza con el de 10:00-12:00
        PlanGenerado plan = plan(dia(COMPLETO,
                bloque("14:00", "16:00", "LUGAR", "2", null, "x", "Park."),
                bloque("11:00", "13:00", "LUGAR", "1", null, "x", "Overlaps."),
                bloque("10:00", "12:00", "LUGAR", "1", null, "x", "First.")));

        // Act
        ValidadorPlanService.Resultado resultado = validar(plan);

        // Assert
        assertThat(bloques(resultado, COMPLETO)).extracting(BloquePlanResponse::motivo).containsExactly("First.", "Park.");
        assertThat(resultado.bloquesEliminados()).isEqualTo(1);
    }

    // ===== Llegada y salida =====
    @Test
    void validadorPlan_validar_llegadaAntesDeLas15YSalidaDespuesDeLas12_seEliminan() {
        // Arrange
        PlanGenerado plan = plan(
                dia(LLEGADA,
                        bloque("13:00", "14:30", "LUGAR", "1", null, "x", "Before check-in."),
                        bloque("16:00", "17:00", "LUGAR", "1", null, "x", "After check-in.")),
                dia(SALIDA,
                        bloque("11:00", "13:00", "LUGAR", "1", null, "x", "After check-out."),
                        bloque("09:00", "11:00", "LUGAR", "2", null, "x", "Before check-out.")));

        // Act
        ValidadorPlanService.Resultado resultado = validar(plan);

        // Assert: el día de llegada empieza con la nota de check-in que agrega el servidor
        List<BloquePlanResponse> llegada = bloques(resultado, LLEGADA);
        assertThat(llegada).extracting(BloquePlanResponse::motivo)
                .containsExactly(ValidadorPlanService.NOTA_CHECK_IN, "After check-in.");
        assertThat(llegada.get(0).tipo()).isEqualTo(TipoBloque.TIEMPO_LIBRE);
        assertThat(llegada.get(0).inicio()).isEqualTo("15:00");
        assertThat(llegada.get(0).fin()).isEqualTo("15:30");
        assertThat(bloques(resultado, SALIDA)).extracting(BloquePlanResponse::motivo).containsExactly("Before check-out.");
        assertThat(resultado.bloquesEliminados()).isEqualTo(2);
    }

    // ===== Fechas =====
    @Test
    void validadorPlan_validar_diasQueFaltanSeCompletanYLosQueSobranSeQuitan() {
        // Arrange: falta el 14 y sobra el 20
        PlanGenerado plan = plan(
                dia(LLEGADA, bloque("16:00", "17:00", "LUGAR", "1", null, "x", "a")),
                dia(COMPLETO, bloque("10:00", "12:00", "LUGAR", "1", null, "x", "b")),
                dia(SALIDA, bloque("09:00", "10:00", "LUGAR", "1", null, "x", "c")),
                dia(LocalDate.of(2026, 12, 20), bloque("10:00", "12:00", "LUGAR", "1", null, "x", "extra")));

        // Act
        ValidadorPlanService.Resultado resultado = validar(plan);

        // Assert
        assertThat(resultado.dias()).extracting(DiaPlanResponse::fecha).containsExactlyElementsOf(DIAS);
        assertThat(resultado.dias()).extracting(DiaPlanResponse::tipoDia)
                .containsExactly("LLEGADA", "COMPLETO", "COMPLETO", "SALIDA");
        assertThat(resultado.dias()).extracting(DiaPlanResponse::numero).containsExactly(1, 2, 3, 4);
        List<BloquePlanResponse> faltante = bloques(resultado, LLUVIOSO);
        assertThat(faltante).hasSize(1);
        assertThat(faltante.get(0).tipo()).isEqualTo(TipoBloque.TIEMPO_LIBRE);
        assertThat(faltante.get(0).titulo()).isEmpty();
        assertThat(faltante.get(0).url()).isNull();
        assertThat(resultado.bloquesEliminados()).isZero();
        // Cada día lleva su clima
        assertThat(resultado.dias().get(2).clima().probLluvia()).isEqualTo(70);
        assertThat(resultado.dias().get(2).clima().tipo()).isEqualTo(TipoClima.CLIMA_TIPICO);
    }

    @Test
    void validadorPlan_validar_tiempoLibreConservaLaNotaDeLaIa() {
        // Arrange
        PlanGenerado plan = plan(dia(COMPLETO,
                bloque("13:00", "14:00", "TIEMPO_LIBRE", "", null, "", "Lunch is yours to choose on Madison Avenue.")));

        // Act
        BloquePlanResponse bloque = soloBloque(validar(plan), COMPLETO);

        // Assert
        assertThat(bloque.tipo()).isEqualTo(TipoBloque.TIEMPO_LIBRE);
        assertThat(bloque.motivo()).isEqualTo("Lunch is yours to choose on Madison Avenue.");
        assertThat(bloque.traslado()).isNull();
        assertThat(bloque.fuente()).isNull();
    }

    @Test
    void validadorPlan_validar_tipoDesconocido_seElimina() {
        // Arrange
        PlanGenerado plan = plan(dia(COMPLETO,
                bloque("10:00", "11:00", "HELICOPTERO", "1", null, "x", "Unknown type."),
                bloque("12:00", "13:00", "LUGAR", "1", null, "x", "ok")));

        // Act & Assert
        assertThat(validar(plan).bloquesEliminados()).isEqualTo(1);
    }

    // ===== Reintento al superar el 40 % =====
    @Test
    void validadorPlan_generarValidado_masDel40PorCiento_reintentaUnaVezYUsaElSegundo() {
        // Arrange: el primero tiene 3 de 4 bloques inventados; el segundo es válido
        PlanGenerado malo = plan(dia(COMPLETO,
                bloque("08:00", "09:00", "LUGAR", "90", null, "x", "fake"),
                bloque("10:00", "11:00", "LUGAR", "91", null, "x", "fake"),
                bloque("12:00", "13:00", "LUGAR", "92", null, "x", "fake"),
                bloque("14:00", "15:00", "LUGAR", "1", null, "x", "ok")));
        PlanGenerado bueno = plan(dia(COMPLETO, bloque("10:00", "12:00", "LUGAR", "1", null, "x", "good")));
        List<PlanGenerado> respuestas = new ArrayList<>(List.of(malo, bueno));
        AtomicInteger llamadas = new AtomicInteger();

        // Act
        ValidadorPlanService.Resultado resultado = validador.generarValidado(() -> {
            llamadas.incrementAndGet();
            return new PlanGenerado.Respuesta(respuestas.remove(0), "modelo-x");
        }, contexto, ESTADIA, List.of(COMPLETO), preferencias());

        // Assert
        assertThat(llamadas.get()).isEqualTo(2);
        assertThat(resultado.modelo()).isEqualTo("modelo-x");
        assertThat(bloques(resultado, COMPLETO)).extracting(BloquePlanResponse::motivo).containsExactly("good");
    }

    @Test
    void validadorPlan_generarValidado_dosVecesMasDel40PorCiento_lanzaPlanNoConfiable() {
        // Arrange: 1 de 2 eliminado = 50 %
        PlanGenerado malo = plan(dia(COMPLETO,
                bloque("10:00", "11:00", "LUGAR", "99", null, "x", "fake"),
                bloque("12:00", "13:00", "LUGAR", "1", null, "x", "ok")));
        AtomicInteger llamadas = new AtomicInteger();

        // Act & Assert
        assertThatThrownBy(() -> validador.generarValidado(() -> {
            llamadas.incrementAndGet();
            return new PlanGenerado.Respuesta(malo, "m");
        }, contexto, ESTADIA, List.of(COMPLETO), preferencias()))
                .isInstanceOf(PlanNoConfiableException.class)
                .hasMessage("We could not build a reliable plan. Please try again.");
        assertThat(llamadas.get()).isEqualTo(2);
    }

    @Test
    void validadorPlan_generarValidado_exactamente40PorCiento_seAcepta() {
        // Arrange: 2 de 5 eliminados = 40 %
        PlanGenerado plan = plan(dia(COMPLETO,
                bloque("08:00", "09:00", "LUGAR", "98", null, "x", "fake"),
                bloque("09:30", "10:00", "LUGAR", "99", null, "x", "fake"),
                bloque("10:00", "11:00", "LUGAR", "1", null, "x", "ok"),
                bloque("12:00", "13:00", "LUGAR", "2", null, "x", "ok"),
                bloque("14:00", "15:00", "RESTAURANTE_HOTEL", "SIGNATURE_RESTAURANT", null, "x", "ok")));
        AtomicInteger llamadas = new AtomicInteger();

        // Act
        validador.generarValidado(() -> {
            llamadas.incrementAndGet();
            return new PlanGenerado.Respuesta(plan, "m");
        }, contexto, ESTADIA, List.of(COMPLETO), preferencias());

        // Assert
        assertThat(llamadas.get()).isEqualTo(1);
    }

    // ===== Ayudas =====
    private ValidadorPlanService.Resultado validar(PlanGenerado plan) {
        return validador.validar(plan, contexto, ESTADIA, DIAS, preferencias(), "modelo-prueba");
    }

    private static List<BloquePlanResponse> bloques(ValidadorPlanService.Resultado resultado, LocalDate fecha) {
        List<BloquePlanResponse> bloques = resultado.dias().stream().filter(d -> d.fecha().equals(fecha))
                .findFirst().orElseThrow().bloques();
        // La nota de check-in del día de llegada no cuenta para las pruebas de otros días
        return fecha.equals(LLEGADA) ? bloques : bloques.stream().toList();
    }

    private static BloquePlanResponse soloBloque(ValidadorPlanService.Resultado resultado, LocalDate fecha) {
        List<BloquePlanResponse> bloques = bloques(resultado, fecha);
        assertThat(bloques).hasSize(1);
        return bloques.get(0);
    }

    private static PlanGenerado plan(DiaGenerado... dias) {
        return new PlanGenerado("Resumen", Arrays.asList(dias));
    }

    private static DiaGenerado dia(LocalDate fecha, BloqueGenerado... bloques) {
        return new DiaGenerado(fecha.toString(), "Título", Arrays.asList(bloques));
    }

    private static BloqueGenerado bloque(String inicio, String fin, String tipo, String referencia, String caracteristica,
                                         String titulo, String motivo) {
        return new BloqueGenerado(inicio, fin, tipo, referencia, caracteristica, titulo, motivo);
    }

    private static PreferenciasPlan preferencias() {
        return new PreferenciasPlan(List.of(Interes.MUSEOS), Ritmo.EQUILIBRADO, Presupuesto.ALTO, List.of(), false, false, null);
    }

    private static ContextoPlan contexto() {
        Map<LocalDate, ClimaDia> clima = new LinkedHashMap<>();
        for (LocalDate dia : DIAS) {
            int lluvia = dia.equals(LLUVIOSO) ? 70 : 10;
            clima.put(dia, new ClimaDia(dia, "Cold", 5, -1, lluvia, TipoClima.CLIMA_TIPICO));
        }
        List<LugarContexto> lugares = List.of(
                new LugarContexto(1L, "The Museum of Modern Art (MoMA)", CategoriaLugar.MUSEO, true, 150,
                        "Daily 10:30-17:30", "https://www.moma.org", 270),
                new LugarContexto(2L, "Central Park", CategoriaLugar.PARQUE, false, 120,
                        "Daily 06:00-01:00", "https://www.centralparknyc.org", 2700));
        EventoCandidato concierto = new EventoCandidato("tm-1", FuentePlan.TICKETMASTER, "Holiday Concert",
                COMPLETO.atTime(19, 30), COMPLETO.atTime(22, 30), "Carnegie Hall", 40.765, -73.98, "MUSICA",
                "https://www.ticketmaster.com/event/1", true);
        List<ServicioContexto> servicios = List.of(new ServicioContexto(1L, "Wellness", 45.0, List.of(MASAJE,
                "Heated Indoor Pool - Open daily 06:00 to 22:00")));
        List<RestauranteContexto> restaurantes = Arrays.stream(RestauranteHotel.values())
                .map(r -> new RestauranteContexto(r, HorariosRestaurantes.nombre(r), HorariosRestaurantes.horario(r), List.of()))
                .toList();
        return new ContextoPlan(DIAS, clima, lugares, List.of(new EventoContexto(concierto, 780.0)), servicios,
                restaurantes, List.of(FuentePlan.TICKETMASTER), List.of());
    }
}
