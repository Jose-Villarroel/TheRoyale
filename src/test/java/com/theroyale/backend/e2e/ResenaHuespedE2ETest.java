package com.theroyale.backend.e2e;

import com.theroyale.backend.model.Cliente;
import com.theroyale.backend.model.EstadoReserva;
import com.theroyale.backend.model.Resena;
import com.theroyale.backend.repository.ClienteRepository;
import com.theroyale.backend.repository.ResenaRepository;
import com.theroyale.backend.repository.ReservaRepository;
import com.theroyale.backend.repository.TipoHabitacionRepository;
import com.theroyale.backend.service.ResenaService;
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
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// ===== Caso 3 (E2E): un huésped reseña su estadía terminada y el admin la oculta =====
//
// Recorrido: un cliente con una estadía FINALIZADA y todavía sin reseña entra a "My Reservations",
// abre su historial y le pone 5 estrellas con un título y un comentario. La tarjeta pasa a ofrecer
// "Edit review" y la reseña aparece primera en la página pública del tipo (/rooms/{tipo}), con el
// promedio y el total actualizados. Luego, en OTRA PESTAÑA, el admin entra a moderar las reseñas
// (/admin/resenas) y la oculta: al recargar la página del tipo ya no se ve, el tipo vuelve a decir
// "No reviews yet", y el autor la sigue viendo en su tarjeta marcada como "Hidden by the hotel".
//
// Se eligió la Presidential Suite porque el DataLoader la deja sin ninguna reseña: así el antes
// ("No reviews yet") y el después (promedio 5.0 con 1 reseña) no dependen de las reseñas de ejemplo.
//
// Igual que los casos 1 y 2, DEFINED_PORT levanta el backend completo (con su DataLoader) en el
// puerto 8080, el que consume el frontend. Para correrlo:
//   1. el backend normal APAGADO (este test ocupa el 8080),
//   2. Angular corriendo aparte:  cd ../TheRoyaleFront && ng serve   (queda en localhost:4200),
//   3. mvn test -Dtest=ResenaHuespedE2ETest
// DirtiesContext cierra la aplicación al terminar: así la base vuelve a sembrarse desde cero (la
// estadía vuelve a quedar sin reseña) y no queda el puerto ocupado.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ResenaHuespedE2ETest {

    private static final String FRONT = "http://localhost:4200";

    // ===== Datos que siembra el DataLoader: Isabella terminó hace dos meses una estadía en la 902
    // (Presidential Suite) y nunca la reseñó; ese tipo no tiene reseñas de nadie.
    private static final String HUESPED_EMAIL = "isabella.lopez@email.com";
    private static final String HUESPED_PASSWORD = "pass1234";
    private static final String ADMIN_EMAIL = "mono@theroyale.com";
    private static final String ADMIN_PASSWORD = "admin123";
    private static final String HABITACION = "902";
    private static final String TIPO = "Presidential Suite";

    // Lo que escribe el huésped. El comentario cumple RN-5 (entre 10 y 1000 caracteres).
    private static final int CALIFICACION = 5;
    private static final String TITULO = "Unforgettable suite";
    private static final String COMENTARIO = "Huge suite, a breathtaking view and impeccable service from the whole staff.";

    // ===== Localizadores: todos por id, agregados en los templates de Angular =====
    private static final By LOGIN_EMAIL = By.id("login-email");
    private static final By LOGIN_PASSWORD = By.id("login-password");
    private static final By LOGIN_ENVIAR = By.id("login-enviar");

    private static final By MIS_RESERVAS_TAB_HISTORIAL = By.id("mis-reservas-tab-historial");

    private static final By FORM_RESENA = By.id("form-resena");
    private static final By RESENA_TITULO = By.id("resena-titulo");
    private static final By RESENA_COMENTARIO = By.id("resena-comentario");
    private static final By BTN_ENVIAR_RESENA = By.id("btn-enviar-resena");

    private static final By RESENAS_ROOM = By.id("resenas-room");
    private static final By RESENAS_PROMEDIO = By.id("resenas-promedio");
    private static final By RESENAS_TOTAL = By.id("resenas-total");
    private static final By RESENAS_VACIO = By.id("resenas-vacio");
    private static final By RESENA_ITEM = By.className("resena-item");

    private static final By NAV_ADMIN_RESENAS = By.id("nav-admin-resenas");

    // La prueba corre en el mismo proceso que el backend, así que puede consultar la base
    // directamente: los ids se buscan por email, número de habitación y nombre del tipo, no se
    // escriben a mano, porque los genera la base al arrancar.
    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private TipoHabitacionRepository tipoHabitacionRepository;

    @Autowired
    private ResenaRepository resenaRepository;

    @Autowired
    private ResenaService resenaService;

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
    void casoTres_huespedResenaSuEstadiaYElAdminLaOculta() {
        long reservaId = idDeEstadiaFinalizada(HUESPED_EMAIL, HABITACION);
        long tipoId = tipoHabitacionRepository.findByNombreIgnoreCase(TIPO)
                .orElseThrow(() -> new AssertionError("El DataLoader no sembró el tipo " + TIPO))
                .getId();

        // Punto de partida: la estadía no tiene reseña y el tipo no tiene ninguna visible
        assertThat(resenaRepository.existsByReservaId(reservaId)).isFalse();
        assertThat(resenaService.resumen(tipoId).total()).isZero();

        // ===== Paso 1: la página pública del tipo todavía no tiene reseñas =====
        driver.get(FRONT + "/rooms/" + tipoId);
        esperarVisible(RESENAS_ROOM);
        assertThat(texto(RESENAS_VACIO)).startsWith("No reviews yet");

        // ===== Paso 2: el huésped inicia sesión y abre su historial de reservas =====
        String pestanaHuesped = driver.getWindowHandle();
        iniciarSesion(HUESPED_EMAIL, HUESPED_PASSWORD, "/profile");
        driver.get(FRONT + "/my-reservations");
        clic(MIS_RESERVAS_TAB_HISTORIAL);
        esperarVisible(By.id("mi-reserva-" + reservaId));
        assertThat(texto(By.id("mi-reserva-estado-" + reservaId))).isEqualTo("FINALIZADA");
        assertThat(texto(By.id("mi-reserva-habitacion-" + reservaId))).isEqualTo("Room " + HABITACION);

        // ===== Paso 3: "Write a review" abre el formulario; elige 5 estrellas, título y comentario =====
        By btnResenar = By.id("btn-resenar-" + reservaId);
        assertThat(texto(btnResenar)).isEqualTo("Write a review");
        clic(btnResenar);
        esperarVisible(FORM_RESENA);

        // Sin calificación el formulario no deja enviar (RN-4)
        assertThat(esperarVisible(BTN_ENVIAR_RESENA).isEnabled()).isFalse();

        clic(By.id("resena-estrella-" + CALIFICACION));
        wait.until(ExpectedConditions.attributeToBe(By.id("resena-estrella-" + CALIFICACION), "aria-checked", "true"));
        escribir(RESENA_TITULO, TITULO);
        escribir(RESENA_COMENTARIO, COMENTARIO);

        // ===== Paso 4: envía la reseña; la tarjeta pasa a "Edit review" =====
        clic(BTN_ENVIAR_RESENA);
        By btnEditar = By.id("btn-editar-resena-" + reservaId);
        esperarVisible(btnEditar);
        assertThat(texto(btnEditar)).isEqualTo("Edit review");
        esperarVisible(By.id("mi-reserva-resena-" + reservaId));
        assertThat(driver.findElements(btnResenar)).as("ya no ofrece reseñar dos veces").isEmpty();
        assertThat(driver.findElements(FORM_RESENA)).as("el formulario se cierra al publicar").isEmpty();

        // La reseña quedó guardada, visible y ligada a la estadía y al tipo correctos
        Resena guardada = resenaRepository.findByReservaId(reservaId)
                .orElseThrow(() -> new AssertionError("No se guardó la reseña de la reserva " + reservaId));
        long resenaId = guardada.getId();
        assertThat(guardada.getCalificacion()).isEqualTo(CALIFICACION);
        assertThat(guardada.getTitulo()).isEqualTo(TITULO);
        assertThat(guardada.getComentario()).isEqualTo(COMENTARIO);
        assertThat(guardada.isVisible()).isTrue();

        // ===== Paso 5: la reseña aparece primera en /rooms/{tipo}, con promedio 5.0 y 1 reseña =====
        driver.get(FRONT + "/rooms/" + tipoId);
        By resenaEnRoom = By.id("resena-" + resenaId);
        esperarVisible(resenaEnRoom);
        assertThat(texto(RESENAS_PROMEDIO)).isEqualTo("5.0");
        assertThat(texto(RESENAS_TOTAL)).isEqualTo("1 review");
        assertThat(driver.findElements(RESENAS_VACIO)).as("con una reseña visible ya no dice \"No reviews yet\"").isEmpty();

        List<WebElement> resenas = driver.findElements(RESENA_ITEM);
        assertThat(resenas.get(0).getAttribute("id"))
                .as("la reseña recién publicada es la primera de la lista")
                .isEqualTo("resena-" + resenaId);
        String tarjeta = resenas.get(0).getAttribute("textContent");
        assertThat(tarjeta).contains(TITULO).contains(COMENTARIO).contains("Isabella");
        // Privacidad (criterio 10): la página pública no muestra el email del autor
        assertThat(tarjeta).doesNotContain(HUESPED_EMAIL);

        // ===== Paso 6: en otra pestaña entra el admin y abre la moderación de reseñas =====
        // La sesión vive en sessionStorage, que es propio de cada pestaña: las dos conviven.
        driver.switchTo().newWindow(WindowType.TAB);
        iniciarSesion(ADMIN_EMAIL, ADMIN_PASSWORD, "/admin/reservas");
        clic(NAV_ADMIN_RESENAS);
        wait.until(ExpectedConditions.urlContains("/admin/resenas"));

        By estadoAdmin = By.id("admin-resena-estado-" + resenaId);
        esperarVisible(By.id("admin-resena-" + resenaId));
        assertThat(texto(estadoAdmin)).isEqualTo("Visible");

        // ===== Paso 7: la oculta con "Hide"; la tarjeta pasa a "Hidden" y ofrece "Show" =====
        clic(By.id("btn-ocultar-resena-" + resenaId));
        wait.until(condicion -> texto(estadoAdmin).equals("Hidden"));
        esperarVisible(By.id("btn-mostrar-resena-" + resenaId));
        assertThat(driver.findElements(By.id("btn-ocultar-resena-" + resenaId))).isEmpty();

        // En la base la reseña sigue existiendo, pero oculta y fuera del resumen del tipo (RN-8)
        assertThat(resenaService.obtenerPorId(resenaId).isVisible()).isFalse();
        assertThat(resenaService.resumen(tipoId).total()).isZero();
        assertThat(resenaService.resumen(tipoId).promedio()).isNull();

        // ===== Paso 8: al recargar /rooms/{tipo} la reseña ya no aparece: "No reviews yet" =====
        driver.switchTo().window(pestanaHuesped);
        driver.get(FRONT + "/rooms/" + tipoId);
        esperarVisible(RESENAS_ROOM);
        assertThat(texto(RESENAS_VACIO)).startsWith("No reviews yet");
        assertThat(driver.findElements(resenaEnRoom)).isEmpty();
        assertThat(driver.findElements(RESENAS_PROMEDIO)).isEmpty();

        // ===== Paso 9: el autor la sigue viendo en su tarjeta, marcada como oculta por el hotel =====
        driver.get(FRONT + "/my-reservations");
        clic(MIS_RESERVAS_TAB_HISTORIAL);
        assertThat(texto(By.id("mi-reserva-resena-oculta-" + reservaId))).isEqualTo("Hidden by the hotel");
    }

    // ===== Id de la estadía FINALIZADA de un cliente en una habitación, leído de la base =====
    private long idDeEstadiaFinalizada(String email, String numeroHabitacion) {
        Cliente cliente = clienteRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AssertionError("El DataLoader no sembró al cliente " + email));
        return reservaRepository.findByClienteIdAndEstadoInOrderByFechaInicioDesc(
                        cliente.getId(), List.of(EstadoReserva.FINALIZADA)).stream()
                .filter(reserva -> reserva.getHabitacion().getNumero().equals(numeroHabitacion))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        email + " no tiene una estadía terminada en la habitación " + numeroHabitacion))
                .getId();
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
}
