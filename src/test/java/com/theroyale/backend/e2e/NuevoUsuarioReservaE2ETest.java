package com.theroyale.backend.e2e;

import com.theroyale.backend.model.EstadoHabitacion;
import com.theroyale.backend.model.EstadoReserva;
import com.theroyale.backend.model.Habitacion;
import com.theroyale.backend.repository.HabitacionRepository;
import com.theroyale.backend.repository.TipoHabitacionRepository;
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
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

// ===== Caso 1 (E2E): usuario nuevo que se registra y hace dos reservas que se cruzan =====
// DEFINED_PORT levanta el backend completo (con su DataLoader) en el puerto 8080, el que usa el
// frontend. Angular debe estar corriendo aparte en localhost:4200 y el backend normal apagado.
// DirtiesContext cierra la aplicación al terminar para no dejar datos ni el puerto ocupado.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class NuevoUsuarioReservaE2ETest {

    private static final String FRONT = "http://localhost:4200";

    // ===== Localizadores: todos por id, agregados en los templates de Angular =====
    private static final By REGISTRO_NOMBRE = By.id("registro-nombre");
    private static final By REGISTRO_APELLIDO = By.id("registro-apellido");
    private static final By REGISTRO_EMAIL = By.id("registro-email");
    private static final By REGISTRO_CEDULA = By.id("registro-cedula");
    private static final By REGISTRO_PASSWORD = By.id("registro-password");
    private static final By REGISTRO_CONFIRMAR = By.id("registro-confirmar-password");
    private static final By REGISTRO_ENVIAR = By.id("registro-enviar");
    private static final By REGISTRO_ERROR_EMAIL = By.id("registro-error-email");
    private static final By LOGIN_EXITO = By.id("login-exito");
    private static final By LOGIN_EMAIL = By.id("login-email");
    private static final By LOGIN_PASSWORD = By.id("login-password");
    private static final By LOGIN_ENVIAR = By.id("login-enviar");
    private static final By RESERVA_CONFIRMAR = By.id("reserva-confirmar");
    private static final By RESERVA_CONFIRMADA = By.id("reserva-confirmada");
    private static final By RESERVA_ESTADO = By.id("reserva-estado");
    private static final By RESERVA_HABITACION = By.id("reserva-habitacion");

    private static final String PASSWORD = "Clave1234";

    // Extrae el número de "Room 101 · Normal"
    private static final Pattern NUMERO_HABITACION = Pattern.compile("Room\\s+(\\S+)");

    // Mismas reglas que usa ReservaService para decidir qué habitaciones se pueden reservar.
    // Si esas reglas cambian en el servicio, hay que cambiarlas también aquí.
    private static final List<EstadoHabitacion> HABITACION_NO_UTILIZABLE =
            List.of(EstadoHabitacion.MANTENIMIENTO, EstadoHabitacion.DESHABILITADA);
    private static final List<EstadoReserva> RESERVA_BLOQUEANTE =
            List.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA, EstadoReserva.EN_CURSO);

    // La prueba corre en el mismo proceso que el backend, así que puede consultar la base directamente
    @Autowired
    private HabitacionRepository habitacionRepository;

    @Autowired
    private TipoHabitacionRepository tipoHabitacionRepository;

    private WebDriver driver;
    private WebDriverWait wait;

    // ===== Se ejecuta antes de CADA prueba: abre un Chrome nuevo =====
    @BeforeEach
    void setUp() {
        ChromeOptions opciones = new ChromeOptions();
        opciones.addArguments("--disable-extensions");
        // opciones.addArguments("--headless=new"); // descomentar para correr sin ventana visible
        driver = new ChromeDriver(opciones);
        // Los datos llegan del backend después del HTML: cada interacción espera hasta 10 s
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    // ===== Cierra el navegador al terminar, pase o falle la prueba =====
    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    void casoUno_usuarioNuevo_registroYDosReservasQueSeCruzan() {
        // Datos únicos por ejecución: email y cédula son únicos en la base
        long sufijo = System.currentTimeMillis();
        String email = "qa." + sufijo + "@email.com";
        String cedula = "CC" + sufijo;

        // ===== Paso 1: el usuario llega al landing =====
        driver.get(FRONT);
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector("h1"), "Stay Above"));

        // ===== Paso 2: intenta registrarse con un correo inválido =====
        driver.get(FRONT + "/register");
        escribir(REGISTRO_NOMBRE, "Laura");
        escribir(REGISTRO_APELLIDO, "Prueba");
        escribir(REGISTRO_EMAIL, "correo-invalido");
        escribir(REGISTRO_CEDULA, cedula);
        escribir(REGISTRO_PASSWORD, PASSWORD);
        escribir(REGISTRO_CONFIRMAR, PASSWORD);
        clic(REGISTRO_ENVIAR);

        // Se verifica el mensaje de error y que no pasó a la pantalla de login
        esperarVisible(REGISTRO_ERROR_EMAIL);
        assertThat(texto(REGISTRO_ERROR_EMAIL)).isEqualTo("Enter a valid email.");
        assertThat(driver.getCurrentUrl()).endsWith("/register");

        // ===== Paso 3: corrige el correo y se registra =====
        escribir(REGISTRO_EMAIL, email);
        clic(REGISTRO_ENVIAR);

        // Tras registrarse lo manda al login con el aviso y el correo ya escrito
        wait.until(ExpectedConditions.urlContains("/login"));
        esperarVisible(LOGIN_EXITO);
        assertThat(texto(LOGIN_EXITO)).isEqualTo("Account created. You can sign in now.");
        wait.until(ExpectedConditions.attributeToBe(LOGIN_EMAIL, "value", email));

        // ===== Paso 4: inicia sesión; un cliente entra a su perfil =====
        escribir(LOGIN_PASSWORD, PASSWORD);
        clic(LOGIN_ENVIAR);
        wait.until(ExpectedConditions.urlContains("/profile"));

        // ===== Paso 5: primera reserva para la semana siguiente =====
        // El tipo se busca por nombre y no por id fijo: los ids los genera la base al arrancar
        Long tipoId = tipoHabitacionRepository.findAll().stream()
                .filter(tipo -> tipo.getNombre().equals("Normal"))
                .findFirst().orElseThrow().getId();
        LocalDate hoy = LocalDate.now();
        LocalDate llegada1 = hoy.plusDays(7);
        LocalDate salida1 = hoy.plusDays(10);
        String habitacion1 = reservar(tipoId, "Normal", llegada1, salida1);

        // ===== Paso 6: segunda reserva con fechas que se cruzan con la primera =====
        LocalDate llegada2 = hoy.plusDays(8);
        LocalDate salida2 = hoy.plusDays(11);
        String habitacion2 = reservar(tipoId, "Normal", llegada2, salida2);

        // Se creó la segunda reserva, pero con otra habitación distinta a la primera
        assertThat(habitacion2).isNotEqualTo(habitacion1);

        // ===== Paso 7: la primera habitación ya no está disponible en esas fechas =====
        // En la ventana de la segunda reserva (que se cruza con la primera) ninguna de las dos queda libre
        assertThat(numerosDisponibles(tipoId, llegada2, salida2)).doesNotContain(habitacion1, habitacion2);
        // Y en las fechas de la primera reserva, la primera habitación tampoco
        assertThat(numerosDisponibles(tipoId, llegada1, salida1)).doesNotContain(habitacion1);
    }

    // ===== Hace una reserva desde /book y devuelve el número de habitación asignado =====
    // La URL ya trae fechas, huéspedes y tipo: Angular los lee y deja el formulario listo, así no se
    // escribe en los campos de fecha (la parte más frágil de Selenium). La sesión del usuario sigue
    // activa porque vive en sessionStorage, que se conserva al navegar dentro de la misma pestaña.
    private String reservar(Long tipoId, String nombreTipo, LocalDate llegada, LocalDate salida) {
        driver.get(FRONT + "/book?fechaInicio=" + llegada + "&fechaFin=" + salida
                + "&huespedes=2&tipoId=" + tipoId);

        // El botón se habilita solo cuando ya llegó la disponibilidad y el tipo quedó seleccionado
        clic(RESERVA_CONFIRMAR);

        // La confirmación aparece en la misma página, sin navegar a otro listado
        esperarVisible(RESERVA_CONFIRMADA);
        // Toda reserva nueva nace PENDIENTE: la estadía todavía no ha empezado
        assertThat(texto(RESERVA_ESTADO)).isEqualTo("PENDIENTE");

        String detalle = texto(RESERVA_HABITACION);
        assertThat(detalle).contains(nombreTipo);
        Matcher numero = NUMERO_HABITACION.matcher(detalle);
        assertThat(numero.find()).as("No se encontró el número de habitación en: " + detalle).isTrue();
        return numero.group(1);
    }

    // ===== Números de habitación libres de un tipo en un rango, según la base de datos =====
    private List<String> numerosDisponibles(Long tipoId, LocalDate inicio, LocalDate fin) {
        List<Habitacion> libres = habitacionRepository.buscarDisponibles(
                tipoId, inicio, fin, HABITACION_NO_UTILIZABLE, RESERVA_BLOQUEANTE, null);
        return libres.stream().map(Habitacion::getNumero).toList();
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
}