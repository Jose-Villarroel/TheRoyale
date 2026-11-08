package com.theroyale.backend.e2e;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

// ===== Caso 1 (E2E): usuario nuevo que se registra y reserva =====
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

    private static final String PASSWORD = "Clave1234";

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
        driver.quit();
    }

    @Test
    void casoUno_usuarioNuevo_registroConCorreoInvalidoYLuegoValido() {
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
        assertThat(esperarVisible(REGISTRO_ERROR_EMAIL).getText()).isEqualTo("Enter a valid email.");
        assertThat(driver.getCurrentUrl()).endsWith("/register");

        // ===== Paso 3: corrige el correo y se registra =====
        escribir(REGISTRO_EMAIL, email);
        clic(REGISTRO_ENVIAR);

        // Tras registrarse lo manda al login con el aviso y el correo ya escrito
        wait.until(ExpectedConditions.urlContains("/login"));
        assertThat(esperarVisible(LOGIN_EXITO).getText()).isEqualTo("Account created. You can sign in now.");
        wait.until(ExpectedConditions.attributeToBe(LOGIN_EMAIL, "value", email));

        // ===== Paso 4: inicia sesión; un cliente entra a su perfil =====
        escribir(LOGIN_PASSWORD, PASSWORD);
        clic(LOGIN_ENVIAR);
        wait.until(ExpectedConditions.urlContains("/profile"));

        // ===== Paso 5 en adelante (reservas): pendiente de los templates de /book =====
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
        wait.until(ExpectedConditions.elementToBeClickable(localizador)).click();
    }
}