package com.theroyale.backend.service.plan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theroyale.backend.dto.plan.InterpretacionPlan;
import com.theroyale.backend.errors.PlanNoConfiableException;
import com.theroyale.backend.errors.ServicioNoDisponibleException;
import com.theroyale.backend.model.EtiquetaPlato;
import com.theroyale.backend.model.Interes;
import com.theroyale.backend.model.Presupuesto;
import com.theroyale.backend.model.Ritmo;
import com.theroyale.backend.service.chat.GeminiClient;
import com.theroyale.backend.service.chat.GeminiClient.RespuestaJson;
import com.theroyale.backend.service.chat.GeminiClient.SolicitudGemini;
import com.theroyale.backend.service.chat.GeminiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// ===== Pruebas de InterpretePlanService con un Gemini simulado (Mockito) =====
// Se comprueba qué se le manda a Gemini y cómo se normaliza lo que devuelve.
@ExtendWith(MockitoExtension.class)
class InterpretePlanServiceTest {

    private static final DatosEstadia ESTADIA =
            new DatosEstadia(LocalDate.of(2026, 12, 12), LocalDate.of(2026, 12, 15), 2, "Luxury");

    @Mock
    private GeminiClient geminiClient;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private InterpretePlanService interpretePlanService;

    @Test
    void interpretePlan_interpretar_devuelvePreferenciasYEntendidoEnElIdiomaDelHuesped() {
        // Arrange
        when(geminiClient.generarJson(any(SolicitudGemini.class), anyMap())).thenReturn(new RespuestaJson("""
                {"preferencias":{"intereses":["MUSEOS","GASTRONOMIA"],"ritmo":"TRANQUILO","presupuesto":"SIN_LIMITE",
                 "dieta":["VEGETARIANO"],"movilidadReducida":false,"conNinos":false,"notas":"Es nuestro aniversario."},
                 "entendido":[{"titulo":"Un aniversario.","detalle":"Reservo la noche del sábado para cenar en el Signature Restaurant."},
                              {"titulo":"Mañanas tranquilas.","detalle":"Ningún día empieza antes de las 10:00."},
                              {"titulo":"Museos y buena mesa.","detalle":"Dos mañanas de museo y comidas vegetarianas."}]}
                """, "gemini-3.1-flash-lite"));

        // Act
        InterpretacionPlan resultado = interpretePlanService.interpretar(
                "Es nuestro aniversario, queremos ir con calma, museos y buena comida vegetariana.", ESTADIA);

        // Assert
        assertThat(resultado.preferencias().intereses()).containsExactly(Interes.MUSEOS, Interes.GASTRONOMIA);
        assertThat(resultado.preferencias().ritmo()).isEqualTo(Ritmo.TRANQUILO);
        assertThat(resultado.preferencias().presupuesto()).isEqualTo(Presupuesto.SIN_LIMITE);
        assertThat(resultado.preferencias().dieta()).containsExactly(EtiquetaPlato.VEGETARIANO);
        assertThat(resultado.preferencias().notas()).isEqualTo("Es nuestro aniversario.");
        assertThat(resultado.entendido()).hasSize(3);
        assertThat(resultado.entendido().get(0).titulo()).isEqualTo("Un aniversario.");
    }

    @Test
    void interpretePlan_interpretar_mandaSoloLaDescripcionYLasFechasConEsquemaJson() {
        // Arrange
        when(geminiClient.generarJson(any(SolicitudGemini.class), anyMap())).thenReturn(new RespuestaJson(
                "{\"preferencias\":{\"intereses\":[\"PARQUES\"]},\"entendido\":[]}", "m"));
        ArgumentCaptor<SolicitudGemini> solicitud = ArgumentCaptor.forClass(SolicitudGemini.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> esquema = ArgumentCaptor.forClass(Map.class);

        // Act
        interpretePlanService.interpretar("We love long walks in the park.", ESTADIA);

        // Assert
        verify(geminiClient).generarJson(solicitud.capture(), esquema.capture());
        String mensaje = solicitud.getValue().contenidos().get(0).partes().get(0).texto();
        assertThat(mensaje).contains("We love long walks in the park.").contains("2026-12-12").contains("\"noches\":3");
        assertThat(solicitud.getValue().instruccionSistema()).contains("What I understood");
        assertThat(esquema.getValue()).containsEntry("type", "OBJECT");
        assertThat(esquema.getValue().toString()).contains("VIDA_NOCTURNA").contains("SIN_GLUTEN");
    }

    @Test
    void interpretePlan_interpretar_valoresFueraDelContrato_seNormalizan() {
        // Arrange: intereses repetidos, inventados y más de 5; dieta PICANTE; 6 frases; notas largas
        String notasLargas = "x".repeat(400);
        when(geminiClient.generarJson(any(SolicitudGemini.class), anyMap())).thenReturn(new RespuestaJson("""
                {"preferencias":{"intereses":["MUSEOS","MUSEOS","OPERA","TEATRO","PARQUES","COMPRAS","FAMILIA","BIENESTAR"],
                 "ritmo":"LENTO","presupuesto":null,"dieta":["PICANTE","SIN_GLUTEN"],"movilidadReducida":true,
                 "conNinos":null,"notas":"%s"},
                 "entendido":[{"titulo":"1.","detalle":"a"},{"titulo":"2.","detalle":"b"},{"titulo":"3.","detalle":"c"},
                              {"titulo":"4.","detalle":"d"},{"titulo":"5.","detalle":"e"},{"titulo":"6.","detalle":"f"}]}
                """.formatted(notasLargas), "m"));

        // Act
        InterpretacionPlan resultado = interpretePlanService.interpretar("Some long description of our trip.", ESTADIA);

        // Assert
        assertThat(resultado.preferencias().intereses())
                .containsExactly(Interes.MUSEOS, Interes.TEATRO, Interes.PARQUES, Interes.COMPRAS, Interes.FAMILIA);
        assertThat(resultado.preferencias().ritmo()).isEqualTo(Ritmo.EQUILIBRADO);
        assertThat(resultado.preferencias().presupuesto()).isEqualTo(Presupuesto.ALTO);
        assertThat(resultado.preferencias().dieta()).containsExactly(EtiquetaPlato.SIN_GLUTEN);
        assertThat(resultado.preferencias().movilidadReducida()).isTrue();
        assertThat(resultado.preferencias().conNinos()).isFalse();
        assertThat(resultado.preferencias().notas()).hasSize(300);
        assertThat(resultado.entendido()).hasSize(5);
    }

    @Test
    void interpretePlan_interpretar_menosDeTresFrases_seCompletaHastaTres() {
        // Arrange
        when(geminiClient.generarJson(any(SolicitudGemini.class), anyMap())).thenReturn(new RespuestaJson(
                "{\"preferencias\":{\"intereses\":[]},\"entendido\":[{\"titulo\":\"A short trip.\",\"detalle\":\"Three days.\"}]}", "m"));

        // Act
        InterpretacionPlan resultado = interpretePlanService.interpretar("Just a short trip to the city.", ESTADIA);

        // Assert
        assertThat(resultado.entendido()).hasSize(3);
        assertThat(resultado.entendido().get(0).titulo()).isEqualTo("A short trip.");
        assertThat(resultado.preferencias().intereses()).containsExactly(Interes.MUSEOS, Interes.PARQUES);
    }

    @Test
    void interpretePlan_interpretar_geminiFalla_lanzaServicioNoDisponible() {
        // Arrange
        when(geminiClient.generarJson(any(SolicitudGemini.class), anyMap())).thenThrow(new GeminiException("503"));

        // Act & Assert
        assertThatThrownBy(() -> interpretePlanService.interpretar("A trip with friends.", ESTADIA))
                .isInstanceOf(ServicioNoDisponibleException.class)
                .hasMessage("The planner is unavailable right now.");
    }

    @Test
    void interpretePlan_interpretar_jsonIlegible_lanzaPlanNoConfiable() {
        // Arrange
        when(geminiClient.generarJson(any(SolicitudGemini.class), anyMap())).thenReturn(new RespuestaJson("{no es json", "m"));

        // Act & Assert
        assertThatThrownBy(() -> interpretePlanService.interpretar("A trip with friends.", ESTADIA))
                .isInstanceOf(PlanNoConfiableException.class);
    }
}
