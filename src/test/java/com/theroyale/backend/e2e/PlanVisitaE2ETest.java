package com.theroyale.backend.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.theroyale.backend.dto.CuentaResponse;
import com.theroyale.backend.dto.plan.BloquePlanResponse;
import com.theroyale.backend.dto.plan.PlanVisitaResponse;
import com.theroyale.backend.model.Cliente;
import com.theroyale.backend.model.EstadoReserva;
import com.theroyale.backend.model.Reserva;
import com.theroyale.backend.model.Servicio;
import com.theroyale.backend.model.TipoBloque;
import com.theroyale.backend.repository.ClienteRepository;
import com.theroyale.backend.repository.PlanVisitaRepository;
import com.theroyale.backend.repository.ReservaRepository;
import com.theroyale.backend.repository.ServicioRepository;
import com.theroyale.backend.service.CuentaService;
import com.theroyale.backend.service.chat.GeminiClient;
import com.theroyale.backend.service.chat.GeminiException;
import com.theroyale.backend.service.plan.PlanVisitaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

// ===== Caso 4 (E2E): un huésped pide su plan de visita a la IA y agrega un masaje a su cuenta =====
//
// Recorrido: un cliente con una reserva CONFIRMADA (cuenta ya abierta) y todavía sin plan entra a
// "My Reservations" y pulsa "Plan my visit". Escribe su viaje en texto libre y pulsa "Compose my plan":
// mientras el concierge compone, aparece la lista "What I understood" con lo que la IA entendió. Al
// terminar ve la pantalla "plan listo" con el conteo de planes y, con "See my plan", el plan día por
// día: un día por cada fecha de la estadía (llegada..salida), nada antes de las 15:00 el día de llegada
// (check-in) ni después de las 12:00 el de salida (check-out), y el pie con las fuentes usadas. En el
// bloque del spa pulsa "Add to my stay": la tarjeta pasa a "Added to your stay" y en la base la cuenta
// de la reserva tiene un consumo nuevo de Wellness por el precio del servicio.
//
// Se eligió la reserva CONFIRMADA de Camila Díaz en la 901 porque el DataLoader le abre la cuenta y no
// le siembra plan (el plan de ejemplo es de la 703 de Sofia Torres), y ningún otro E2E la usa.
//
// ===== Gemini y fuentes FALSAS: la prueba nunca sale a internet =====
//   - plan.fuentes.habilitadas=false activa los clientes falsos de NWS, Open-Meteo, NYC Open Data y
//     Ticketmaster (un concierto de jazz de muestra cada noche, id "tm-falso-AAAAMMDD").
//   - gemini.api-key=fake-key-e2e solo hace que el planificador se declare disponible: la clave no se
//     envía a ningún lado porque GeminiFalso (@Primary) reemplaza a GeminiRestClient en toda la app.
//   - GeminiFalso distingue las dos llamadas por el esquema de respuesta: si pide "entendido" es la
//     interpretación del texto libre; si pide "dias" es la generación del plan, y arma el JSON SOLO con
//     los IDs que vienen en el contexto que le mandó el backend (servicioId y característica exacta del
//     spa, lugarId de lugares bajo techo y el evento falso de Ticketmaster). Así el validador no
//     descarta nada. Guarda cada solicitud para comprobar la privacidad (criterio 11).
//
// Igual que los casos 1 a 3, DEFINED_PORT levanta el backend completo (con su DataLoader) en el
// puerto 8080, el que consume el frontend. Para correrlo:
//   1. el backend normal APAGADO (este test ocupa el 8080),
//   2. Angular corriendo aparte:  cd ../TheRoyaleFront && ng serve   (queda en localhost:4200),
//   3. mvn test -Dtest=PlanVisitaE2ETest
// DirtiesContext cierra la aplicación al terminar: así la base vuelve a sembrarse desde cero (la
// reserva vuelve a quedar sin plan y sin el masaje) y no queda el puerto ocupado.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {
        "plan.fuentes.habilitadas=false",
        "plan.clima.cron=-",
        "plan.ticketmaster.api-key=",
        "plan.nyc-open-data.app-token=",
        "gemini.api-key=fake-key-e2e"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PlanVisitaE2ETest {

    private static final String FRONT = "http://localhost:4200";

    // ===== Datos que siembra el DataLoader: Camila llega en 5 días a la 901 (4 noches), su reserva está
    // CONFIRMADA con la cuenta abierta y no tiene plan de visita.
    private static final String HUESPED_EMAIL = "camila.diaz@email.com";
    private static final String HUESPED_PASSWORD = "pass1234";
    private static final String HABITACION = "901";
    private static final String SERVICIO_SPA = "Wellness";
    private static final String MASAJE = "Signature Massage";

    // Lo que escribe el huésped (entre 10 y 1000 caracteres, como pide /interpretar)
    private static final String DESCRIPCION = "It is our anniversary. We love museums and good food, "
            + "and we would like a relaxing massage at the hotel spa on the first evening.";

    // Lo que "entendió" el Gemini falso: aparece en la pantalla de composición
    private static final List<String> ENTENDIDO = List.of("An anniversary.", "Museums and good food.", "Time to relax.");

    // ===== Localizadores: todos por id, agregados en los templates de Angular =====
    private static final By LOGIN_EMAIL = By.id("login-email");
    private static final By LOGIN_PASSWORD = By.id("login-password");
    private static final By LOGIN_ENVIAR = By.id("login-enviar");

    private static final By PLAN_DESCRIPCION = By.id("plan-descripcion");
    private static final By BTN_COMPONER_PLAN = By.id("btn-componer-plan");
    private static final By PLAN_ENTENDIDO_LISTA = By.id("plan-entendido-lista");
    private static final By PLAN_ENTENDIDO_ITEM = By.cssSelector("#plan-entendido-lista .plan-entendido-item:not(.plan-entendido-en-curso)");
    private static final By BTN_VER_PLAN = By.id("btn-ver-plan");
    private static final By PLAN_CONTEO = By.id("plan-conteo");
    private static final By PLAN_TITULO = By.id("plan-titulo");
    private static final By PLAN_FUENTES = By.id("plan-fuentes");
    private static final By PLAN_DIA = By.cssSelector("section.plan-dia");
    private static final By BTN_AGREGAR = By.cssSelector("[id^='btn-agregar-']");

    // La prueba corre en el mismo proceso que el backend, así que puede consultar la base
    // directamente: los ids se buscan por email, número de habitación y nombre del servicio, no se
    // escriben a mano, porque los genera la base al arrancar.
    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private ServicioRepository servicioRepository;

    @Autowired
    private PlanVisitaRepository planVisitaRepository;

    @Autowired
    private PlanVisitaService planVisitaService;

    @Autowired
    private CuentaService cuentaService;

    @Autowired
    private GeminiFalso geminiFalso;

    private WebDriver driver;
    private WebDriverWait wait;
    // Componer el plan dura unos segundos (el Gemini falso simula la latencia real)
    private WebDriverWait esperaLarga;

    // ===== Gemini falso que reemplaza a GeminiRestClient en toda la aplicación =====
    @TestConfiguration
    static class ConfiguracionGeminiFalso {

        @Bean
        @Primary
        GeminiFalso geminiFalso(ObjectMapper objectMapper) {
            return new GeminiFalso(objectMapper);
        }
    }

    // ===== Se ejecuta antes de CADA prueba: abre un Chrome nuevo =====
    @BeforeEach
    void setUp() {
        geminiFalso.solicitudes.clear();
        ChromeOptions opciones = new ChromeOptions();
        opciones.addArguments("--disable-extensions");
        // opciones.addArguments("--headless=new"); // descomentar para correr sin ventana visible
        driver = new ChromeDriver(opciones);
        // Los datos llegan del backend después del HTML: cada interacción espera hasta 10 s
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        esperaLarga = new WebDriverWait(driver, Duration.ofSeconds(40));
    }

    // ===== Cierra el navegador al terminar, pase o falle la prueba =====
    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    void casoCuatro_huespedComponeSuPlanConLaIaYAgregaUnMasajeASuCuenta() {
        Cliente cliente = clienteRepository.findByEmailIgnoreCase(HUESPED_EMAIL)
                .orElseThrow(() -> new AssertionError("El DataLoader no sembró al cliente " + HUESPED_EMAIL));
        Reserva reserva = reservaConfirmada(cliente, HABITACION);
        long reservaId = reserva.getId();
        LocalDate llegada = reserva.getFechaInicio();
        LocalDate salida = reserva.getFechaFin();
        Servicio spa = servicioRepository.findByNombreIgnoreCase(SERVICIO_SPA)
                .orElseThrow(() -> new AssertionError("El DataLoader no sembró el servicio " + SERVICIO_SPA));
        BigDecimal precioSpa = BigDecimal.valueOf(spa.getPrecio());

        // Punto de partida: la reserva no tiene plan y su cuenta está abierta
        assertThat(planVisitaRepository.findByReservaId(reservaId)).isEmpty();
        long masajesAntes = consumosDe(reservaId, spa.getId());

        // ===== Paso 1: el huésped inicia sesión y abre "My Reservations" =====
        iniciarSesion(HUESPED_EMAIL, HUESPED_PASSWORD, "/profile");
        driver.get(FRONT + "/my-reservations");
        esperarVisible(By.id("mi-reserva-" + reservaId));
        assertThat(texto(By.id("mi-reserva-estado-" + reservaId))).isEqualTo("CONFIRMADA");

        // ===== Paso 2: "Plan my visit" abre la caja para describir el viaje =====
        By btnPlan = By.id("btn-plan-" + reservaId);
        assertThat(texto(btnPlan)).isEqualTo("Plan my visit");
        clic(btnPlan);
        wait.until(ExpectedConditions.urlContains("/my-reservations/" + reservaId + "/plan"));
        escribir(PLAN_DESCRIPCION, DESCRIPCION);
        wait.until(ExpectedConditions.elementToBeClickable(BTN_COMPONER_PLAN));

        // ===== Paso 3: "Compose my plan"; aparece "What I understood" con lo que entendió la IA =====
        clic(BTN_COMPONER_PLAN);
        esperarVisible(PLAN_ENTENDIDO_LISTA);
        esperaLarga.until(condicion -> driver.findElements(PLAN_ENTENDIDO_ITEM).size() == ENTENDIDO.size());
        String entendido = texto(PLAN_ENTENDIDO_LISTA);
        ENTENDIDO.forEach(item -> assertThat(entendido).contains(item));

        // ===== Paso 4: el plan está listo: conteo de planes y del extra del hotel listo para agregar =====
        esperaLarga.until(ExpectedConditions.elementToBeClickable(BTN_VER_PLAN));
        assertThat(texto(PLAN_CONTEO)).contains("plans").contains("1 hotel extra ready to add");
        clic(BTN_VER_PLAN);

        // ===== Paso 5: el plan detallado tiene un día por cada fecha de la estadía =====
        esperarVisible(PLAN_TITULO);
        List<LocalDate> fechas = new ArrayList<>();
        for (LocalDate fecha = llegada; !fecha.isAfter(salida); fecha = fecha.plusDays(1)) {
            fechas.add(fecha);
        }
        for (LocalDate fecha : fechas) {
            esperarVisible(By.id("plan-dia-" + fecha));
            assertThat(driver.findElements(By.id("nav-dia-" + fecha))).as("navegación del día " + fecha).hasSize(1);
        }
        assertThat(driver.findElements(PLAN_DIA)).as("un día por fecha, de la llegada a la salida").hasSize(fechas.size());

        // Llegada: nada empieza antes del check-in (15:00). Salida: nada termina después del check-out (12:00).
        // Las horas se leen del DOM (las notas de tiempo libre no muestran hora, solo los bloques).
        List<LocalTime> iniciosLlegada = horas(llegada, ".plan-bloque-inicio");
        assertThat(iniciosLlegada).as("bloques con hora el día de llegada").isNotEmpty();
        assertThat(iniciosLlegada).allMatch(hora -> !hora.isBefore(LocalTime.of(15, 0)));
        List<LocalTime> finesSalida = horas(salida, ".plan-bloque-fin");
        assertThat(finesSalida).as("bloques con hora el día de salida").isNotEmpty();
        assertThat(finesSalida).allMatch(hora -> !hora.isAfter(LocalTime.of(12, 0)));
        assertThat(texto(By.id("plan-dia-" + llegada)))
                .contains("Check-in from 15:00. Your suite will be ready when you arrive.");

        // El pie dice de dónde salieron los datos
        assertThat(texto(PLAN_FUENTES)).startsWith("Suggested by our AI concierge with data from");

        // ===== Paso 6: el bloque del spa es el único que se puede agregar a la cuenta =====
        PlanVisitaResponse plan = planVisitaService.obtener(reservaId, cliente.getId());
        BloquePlanResponse masaje = plan.dias().stream()
                .flatMap(dia -> dia.bloques().stream())
                .filter(bloque -> bloque.tipo() == TipoBloque.SERVICIO_HOTEL)
                .findFirst()
                .orElseThrow(() -> new AssertionError("El plan no trae el bloque del spa"));
        assertThat(masaje.servicioId()).isEqualTo(spa.getId());
        assertThat(masaje.titulo()).isEqualTo(MASAJE);
        assertThat(masaje.agregable()).isTrue();
        assertThat(driver.findElements(BTN_AGREGAR)).hasSize(1);

        String bloqueId = masaje.id();
        By bloqueMasaje = By.id("plan-bloque-" + bloqueId);
        By btnAgregar = By.id("btn-agregar-" + bloqueId);
        assertThat(texto(bloqueMasaje)).contains(MASAJE);
        assertThat(texto(By.id("precio-" + bloqueId))).contains("charged to your room");
        assertThat(texto(btnAgregar)).isEqualTo("Add to my stay");

        // ===== Paso 7: "Add to my stay" =====
        clic(btnAgregar);
        assertThat(texto(By.id("agregado-" + bloqueId))).isEqualTo("Added to your stay");
        assertThat(driver.findElements(btnAgregar)).as("ya no ofrece agregarlo dos veces").isEmpty();

        // En la base: la cuenta tiene un consumo más de Wellness, por el precio del servicio
        CuentaResponse cuenta = cuentaService.obtenerRespuestaPorReserva(reservaId);
        List<CuentaResponse.ItemResponse> masajes = cuenta.items().stream()
                .filter(item -> spa.getId().equals(item.servicioId()))
                .toList();
        assertThat(masajes).hasSize((int) masajesAntes + 1);
        CuentaResponse.ItemResponse cargo = masajes.get(masajes.size() - 1);
        assertThat(cargo.cantidad()).isEqualTo(1);
        assertThat(cargo.precioUnitario()).isEqualByComparingTo(precioSpa);
        assertThat(cargo.pagado()).isFalse();

        // El plan quedó guardado y ligado a la reserva
        assertThat(planVisitaRepository.findByReservaId(reservaId)).isPresent();

        // ===== Privacidad (criterio 11): a Gemini no le llegó nada que identifique al huésped =====
        assertThat(geminiFalso.solicitudes).as("una interpretación y una generación").hasSizeGreaterThanOrEqualTo(2);
        String enviado = String.join("\n", geminiFalso.solicitudes);
        assertThat(enviado)
                .doesNotContain(cliente.getNombre())
                .doesNotContain(cliente.getApellido())
                .doesNotContainIgnoringCase(cliente.getEmail())
                .doesNotContain(cliente.getCedula())
                .doesNotContain(cliente.getTelefono());
    }

    // ===== Reserva CONFIRMADA de un cliente en una habitación, leída de la base =====
    private Reserva reservaConfirmada(Cliente cliente, String numeroHabitacion) {
        return reservaRepository.findByClienteIdAndEstadoInOrderByFechaInicioDesc(
                        cliente.getId(), List.of(EstadoReserva.CONFIRMADA)).stream()
                .filter(reserva -> reserva.getHabitacion().getNumero().equals(numeroHabitacion))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        cliente.getEmail() + " no tiene una reserva confirmada en la habitación " + numeroHabitacion));
    }

    private long consumosDe(long reservaId, Long servicioId) {
        return cuentaService.obtenerRespuestaPorReserva(reservaId).items().stream()
                .filter(item -> servicioId.equals(item.servicioId()))
                .count();
    }

    // Horas (HH:mm) de los bloques de un día; el fin se muestra como "to 17:30"
    private List<LocalTime> horas(LocalDate fecha, String clase) {
        return driver.findElements(By.cssSelector("#plan-dia-" + fecha + " " + clase)).stream()
                .map(elemento -> elemento.getAttribute("textContent").replace("to", "").trim())
                .map(LocalTime::parse)
                .toList();
    }

    // ===== Inicia sesión y espera a que el rol aterrice en su propio panel =====
    private void iniciarSesion(String email, String password, String destinoEsperado) {
        driver.get(FRONT + "/login");
        escribir(LOGIN_EMAIL, email);
        escribir(LOGIN_PASSWORD, password);
        clic(LOGIN_ENVIAR);
        wait.until(ExpectedConditions.urlContains(destinoEsperado));
    }

    // ===== Métodos de ayuda: evitan repetir la espera en cada interacción =====
    private WebElement esperarVisible(By localizador) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(localizador));
    }

    private void escribir(By localizador, String texto) {
        WebElement campo = esperarVisible(localizador);
        campo.clear();
        campo.sendKeys(texto);
    }

    private void clic(By localizador) {
        WebElement elemento = wait.until(ExpectedConditions.elementToBeClickable(localizador));
        try {
            elemento.click();
        } catch (Exception e) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", elemento);
        }
    }

    // textContent devuelve el texto tal como está en el HTML, sin que el CSS lo altere (mayúsculas, etc.)
    private String texto(By localizador) {
        return esperarVisible(localizador).getAttribute("textContent").trim();
    }

    // ===== Gemini falso: responde la interpretación y el plan, y guarda lo que se le envió =====
    static class GeminiFalso implements GeminiClient {

        private static final String MODELO = "gemini-falso-e2e";
        // Simula la latencia de Gemini: deja ver la pantalla "composing" con su lista "What I understood"
        private static final long DEMORA_PLAN_MS = 4000;

        private final ObjectMapper objectMapper;
        final List<String> solicitudes = new CopyOnWriteArrayList<>();

        GeminiFalso(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public void transmitir(SolicitudGemini solicitud, Consumer<ParteGemini> alRecibirParte) {
            throw new GeminiException("El chat no forma parte del caso 4.");
        }

        @Override
        @SuppressWarnings("unchecked")
        public RespuestaJson generarJson(SolicitudGemini solicitud, Map<String, Object> esquemaRespuesta) {
            String mensaje = solicitud.contenidos().get(0).partes().get(0).texto();
            solicitudes.add(solicitud.instruccionSistema() + "\n" + mensaje);
            Map<String, Object> propiedades = (Map<String, Object>) esquemaRespuesta.get("properties");
            try {
                if (propiedades.containsKey("entendido")) {
                    return new RespuestaJson(interpretacion(), MODELO);
                }
                Thread.sleep(DEMORA_PLAN_MS);
                return new RespuestaJson(plan(objectMapper.readTree(mensaje)), MODELO);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new GeminiException("Interrumpido");
            } catch (Exception e) {
                throw new GeminiException("El Gemini falso no pudo leer el contexto: " + e.getMessage());
            }
        }

        private String interpretacion() {
            return """
                    {"preferencias": {"intereses": ["MUSEOS", "GASTRONOMIA", "BIENESTAR"], "ritmo": "EQUILIBRADO",
                      "presupuesto": "ALTO", "dieta": [], "movilidadReducida": false, "conNinos": false,
                      "notas": "Anniversary trip, a massage on the first evening."},
                     "entendido": [
                      {"titulo": "%s", "detalle": "I will keep one evening that feels special."},
                      {"titulo": "%s", "detalle": "Each full day starts at a museum, with lunch nearby."},
                      {"titulo": "%s", "detalle": "Your first evening begins with a massage at the hotel spa."}]}
                    """.formatted(ENTENDIDO.get(0), ENTENDIDO.get(1), ENTENDIDO.get(2));
        }

        // Arma el plan SOLO con los IDs del contexto que mandó el backend
        private String plan(JsonNode contexto) {
            JsonNode spa = null;
            for (JsonNode servicio : contexto.path("SERVICIOS")) {
                if (SERVICIO_SPA.equalsIgnoreCase(servicio.path("nombre").asText())) {
                    spa = servicio;
                }
            }
            if (spa == null) {
                throw new GeminiException("El contexto no trae el servicio " + SERVICIO_SPA);
            }
            String caracteristicaMasaje = null;
            for (JsonNode caracteristica : spa.path("caracteristicas")) {
                if (caracteristica.asText().startsWith(MASAJE)) {
                    caracteristicaMasaje = caracteristica.asText();
                }
            }
            List<String> lugaresBajoTecho = new ArrayList<>();
            for (JsonNode lugar : contexto.path("LUGARES")) {
                if (lugar.path("bajoTecho").asBoolean()) {
                    lugaresBajoTecho.add(lugar.path("lugarId").asText());
                }
            }
            List<String> eventos = new ArrayList<>();
            contexto.path("EVENTOS").forEach(evento -> eventos.add(evento.path("eventoId").asText()));

            ObjectNode raiz = objectMapper.createObjectNode();
            raiz.put("resumen", "Happy anniversary. Museums in the mornings, good food and a massage to begin.");
            ArrayNode dias = raiz.putArray("dias");
            int lugar = 0;
            for (JsonNode dia : contexto.path("diasAPlanificar")) {
                String fecha = dia.path("fecha").asText();
                ObjectNode nuevo = dias.addObject();
                nuevo.put("fecha", fecha);
                ArrayNode bloques = nuevo.putArray("bloques");
                switch (dia.path("tipoDia").asText()) {
                    case "LLEGADA" -> {
                        nuevo.put("titulo", "A relaxing first evening");
                        bloque(bloques, "16:00", "17:30", "SERVICIO_HOTEL", spa.path("servicioId").asText(),
                                caracteristicaMasaje, "A massage to start the trip rested.");
                        String evento = "tm-falso-" + fecha.replace("-", "");
                        if (eventos.contains(evento)) {
                            bloque(bloques, "19:30", "22:00", "EVENTO", evento, "", "Live jazz for the anniversary night.");
                        }
                    }
                    case "SALIDA" -> {
                        nuevo.put("titulo", "One last museum");
                        bloque(bloques, "09:30", "11:30", "LUGAR", lugaresBajoTecho.get(lugar++ % lugaresBajoTecho.size()),
                                "", "A calm morning before check-out.");
                    }
                    default -> {
                        nuevo.put("titulo", "Museums and good food");
                        bloque(bloques, "10:00", "12:00", "LUGAR", lugaresBajoTecho.get(lugar++ % lugaresBajoTecho.size()),
                                "", "Start the day indoors, close to the hotel.");
                        bloque(bloques, "12:30", "13:30", "TIEMPO_LIBRE", "", "", "Lunch is yours to choose nearby.");
                        bloque(bloques, "14:30", "16:30", "LUGAR", lugaresBajoTecho.get(lugar++ % lugaresBajoTecho.size()),
                                "", "An afternoon among great collections.");
                    }
                }
            }
            return raiz.toString();
        }

        private static void bloque(ArrayNode bloques, String inicio, String fin, String tipo, String referenciaId,
                                   String caracteristica, String motivo) {
            ObjectNode bloque = bloques.addObject();
            bloque.put("inicio", inicio);
            bloque.put("fin", fin);
            bloque.put("tipo", tipo);
            bloque.put("referenciaId", referenciaId);
            bloque.put("caracteristica", caracteristica);
            bloque.put("titulo", "");
            bloque.put("motivo", motivo);
        }
    }
}
