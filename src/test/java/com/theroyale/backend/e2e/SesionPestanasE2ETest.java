package com.theroyale.backend.e2e;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;
import java.util.List;
import java.util.logging.Level;

import static org.assertj.core.api.Assertions.assertThat;

// ===== Caso 5 (E2E): una sola sesión por navegador, compartida entre pestañas =====
//
// El token vive en localStorage (guía de seguridad, §9.1), que es uno solo para todo el navegador.
// Si en otra pestaña se cierra sesión o entra otro usuario, la pestaña que estaba abierta debe
// ponerse al día sola (login o panel del nuevo rol) en vez de quedar en blanco o mostrar al admin
// mientras envía el token del cliente.
//
// Igual que los otros casos: backend completo en el 8080 (DEFINED_PORT) y Angular en localhost:4200.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SesionPestanasE2ETest {

    private static final String FRONT = "http://localhost:4200";

    private static final String ADMIN_EMAIL = "mono@theroyale.com";
    private static final String ADMIN_PASSWORD = "admin123";
    private static final String HUESPED_EMAIL = "daniel.castro@email.com";
    private static final String HUESPED_PASSWORD = "pass1234";

    private static final By LOGIN_EMAIL = By.id("login-email");
    private static final By LOGIN_PASSWORD = By.id("login-password");
    private static final By LOGIN_ENVIAR = By.id("login-enviar");
    private static final By CERRAR_SESION = By.id("cabecera-cerrar-sesion");
    private static final By NAV_ADMIN_RESENAS = By.id("nav-admin-resenas");

    private WebDriver driver;
    private WebDriverWait wait;

    // ===== Se ejecuta antes de CADA prueba: abre un Chrome nuevo que guarda la consola =====
    @BeforeEach
    void setUp() {
        ChromeOptions opciones = new ChromeOptions();
        opciones.addArguments("--disable-extensions");
        // opciones.addArguments("--headless=new"); // descomentar para correr sin ventana visible
        LoggingPreferences registros = new LoggingPreferences();
        registros.enable(LogType.BROWSER, Level.ALL);
        opciones.setCapability("goog:loggingPrefs", registros);
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
    void casoCinco_otraPestanaCambiaDeUsuario_laPestanaDelAdminSePoneAlDia() {
        // ===== Paso 1: pestaña A, el admin entra a su panel de reservas =====
        iniciarSesion(ADMIN_EMAIL, ADMIN_PASSWORD, "/admin/reservas");
        esperarVisible(NAV_ADMIN_RESENAS);
        String pestanaAdmin = driver.getWindowHandle();

        // ===== Paso 2: pestaña B, se abre el sitio con la misma sesión y se cierra =====
        driver.switchTo().newWindow(WindowType.TAB);
        String pestanaB = driver.getWindowHandle();
        driver.get(FRONT + "/admin/reservas");
        clic(CERRAR_SESION);
        wait.until(ExpectedConditions.urlContains("/login"));

        // ===== Paso 3: la pestaña A se entera del logout y vuelve sola al login =====
        driver.switchTo().window(pestanaAdmin);
        wait.until(ExpectedConditions.urlContains("/login"));
        esperarVisible(LOGIN_EMAIL);

        // ===== Paso 4: en la pestaña B entra un cliente =====
        driver.switchTo().window(pestanaB);
        iniciarSesion(HUESPED_EMAIL, HUESPED_PASSWORD, "/profile");

        // ===== Paso 5: la pestaña A pasa sola al perfil del cliente, con sus datos (no en blanco) =====
        driver.switchTo().window(pestanaAdmin);
        wait.until(ExpectedConditions.urlContains("/profile"));
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("body"), HUESPED_EMAIL));

        // ===== Paso 6: aunque el cliente escriba la URL del admin, el guard lo devuelve a su perfil =====
        driver.get(FRONT + "/admin/reservas");
        wait.until(ExpectedConditions.urlContains("/profile"));
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("body"), HUESPED_EMAIL));

        // Ningún paso dejó una excepción sin atrapar en la consola (las que dejan la página en blanco)
        assertThat(erroresDeJavaScript()).isEmpty();
    }

    // ===== Inicia sesión y espera a que el rol aterrice en su propio panel =====
    private void iniciarSesion(String email, String password, String destinoEsperado) {
        driver.get(FRONT + "/login");
        escribir(LOGIN_EMAIL, email);
        escribir(LOGIN_PASSWORD, password);
        clic(LOGIN_ENVIAR);
        wait.until(ExpectedConditions.urlContains(destinoEsperado));
    }

    // Errores de la consola que no son respuestas HTTP (esas las muestra cada página como aviso)
    private List<String> erroresDeJavaScript() {
        return driver.manage().logs().get(LogType.BROWSER).getAll().stream()
                .filter(entrada -> entrada.getLevel().intValue() >= Level.SEVERE.intValue())
                .map(LogEntry::getMessage)
                .filter(mensaje -> !mensaje.contains("Failed to load resource"))
                .toList();
    }

    private void escribir(By localizador, String texto) {
        esperarVisible(localizador).clear();
        driver.findElement(localizador).sendKeys(texto);
    }

    // Clic por JavaScript: la cabecera puede quedar tapada por el chat flotante en ventanas pequeñas
    private void clic(By localizador) {
        wait.until(ExpectedConditions.presenceOfElementLocated(localizador));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", driver.findElement(localizador));
    }

    private WebElement esperarVisible(By localizador) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(localizador));
    }
}
