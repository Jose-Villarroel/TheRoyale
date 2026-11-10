package com.theroyale.backend.e2e;

import com.theroyale.backend.dto.CuentaResponse;
import com.theroyale.backend.model.EstadoCuenta;
import com.theroyale.backend.model.EstadoHabitacion;
import com.theroyale.backend.model.EstadoReserva;
import com.theroyale.backend.model.Habitacion;
import com.theroyale.backend.repository.HabitacionRepository;
import com.theroyale.backend.repository.ServicioRepository;
import com.theroyale.backend.service.CuentaService;
import com.theroyale.backend.service.ReservaService;
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
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// ===== Caso 2 (E2E): el operador atiende la estadía completa de un huésped ya registrado =====
//
// Recorrido: el huésped entra y ve su reserva pendiente de iniciar; en OTRA PESTAÑA un operador
// inicia sesión, le hace check-in, le carga dos servicios, le cobra todo el saldo y cierra la
// estadía con el check-out.
//
// Igual que el Caso 1, DEFINED_PORT levanta el backend completo (con su DataLoader) en el puerto
// 8080, el que consume el frontend. Para correrlo:
//   1. el backend normal APAGADO (este test ocupa el 8080),
//   2. Angular corriendo aparte:  cd TheRoyale && ng serve   (queda en localhost:4200),
//   3. mvn test -Dtest=EstadiaOperadorE2ETest
// DirtiesContext cierra la aplicación al terminar: así la base vuelve a sembrarse desde cero y no
// queda el puerto ocupado.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class EstadiaOperadorE2ETest {

    private static final String FRONT = "http://localhost:4200";

    // ===== Datos que siembra el DataLoader. Se eligió esta reserva porque es la única que cumple
    // las tres condiciones que exige ReservaService.hacerCheckIn: estado CONFIRMADA, llegada que no
    // es posterior a hoy, y habitación en DISPONIBLE. Además se creó hace 15 días ("tiempo atrás")
    // y todavía no ha empezado, que es justo el punto de partida del enunciado.
    private static final String HUESPED_EMAIL = "carlos.mendoza@email.com";
    private static final String HUESPED_PASSWORD = "pass1234";
    private static final String OPERADOR_EMAIL = "laura.gomez@theroyale.com";
    private static final String OPERADOR_PASSWORD = "operator123";
    private static final String HABITACION = "101";

    // Los dos servicios que el operador le carga a la cuenta durante la estadía
    private static final String SERVICIO_UNO = "Wellness";
    private static final int CANTIDAD_UNO = 1;
    private static final String SERVICIO_DOS = "Dining";
    private static final int CANTIDAD_DOS = 2;

    private static final String METODO_PAGO = "CARD";

    // ===== Localizadores: todos por id, agregados en los templates de Angular =====
    private static final By LOGIN_EMAIL = By.id("login-email");
    private static final By LOGIN_PASSWORD = By.id("login-password");
    private static final By LOGIN_ENVIAR = By.id("login-enviar");

    private static final By MI_RESERVA_ITEM = By.className("mi-reserva-item");
    private static final By MIS_RESERVAS_TAB_HISTORIAL = By.id("mis-reservas-tab-historial");

    private static final By FILTRO_CONFIRMADA = By.id("filtro-CONFIRMADA");

    private static final By OPERADOR_MENSAJE = By.id("operador-mensaje");
    private static final By RESERVA_ESTADO_DETALLE = By.id("reserva-estado-detalle");
    private static final By ACCION_CHECKIN = By.id("accion-checkin");
    private static final By ACCION_CHECKOUT = By.id("accion-checkout");

    private static final By CUENTA_ESTADO = By.id("cuenta-estado");
    private static final By CUENTA_TOTAL_CONSUMOS = By.id("cuenta-total-consumos");
    private static final By CUENTA_TOTAL_PAGADO = By.id("cuenta-total-pagado");
    private static final By CUENTA_SALDO = By.id("cuenta-saldo");
    private static final By CUENTA_SALDADA = By.id("cuenta-saldada");

    private static final By CONSUMO_SERVICIO = By.id("consumo-servicio");
    private static final By CONSUMO_CANTIDAD = By.id("consumo-cantidad");
    private static final By CONSUMO_AGREGAR = By.id("consumo-agregar");
    private static final By CONSUMO_FILA = By.className("consumo-fila");
    private static final By CONSUMO_PAGADO = By.className("consumo-pagado");

    private static final By PAGO_MONTO = By.id("pago-monto");
    private static final By PAGO_METODO = By.id("pago-metodo");
    private static final By PAGO_REGISTRAR = By.id("pago-registrar");
    private static final By PAGO_SALDO_COMPLETO = By.id("pago-saldo-completo");

    // La prueba corre en el mismo proceso que el backend, así que puede consultar la base
    // directamente para comprobar el estado final del sistema y los precios vigentes.
    @Autowired
    private HabitacionRepository habitacionRepository;

    @Autowired
    private ServicioRepository servicioRepository;

    // Se usan los servicios y no los repositorios para la cuenta y la reserva: el mapeo a DTO
    // ocurre dentro de la transacción, donde las listas LAZY de consumos y pagos sí se pueden leer.
    @Autowired
    private CuentaService cuentaService;

    @Autowired
    private ReservaService reservaService;

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
    void casoDos_operadorAtiendeLaEstadiaCompletaDelHuesped() {
        // Precios vigentes leídos de la base: si mañana cambian, el monto esperado cambia con ellos
        BigDecimal precioUno = precioServicio(SERVICIO_UNO);
        BigDecimal precioDos = precioServicio(SERVICIO_DOS);

        // ===== Paso 1: el huésped ya registrado inicia sesión =====
        String pestanaHuesped = driver.getWindowHandle();
        iniciarSesion(HUESPED_EMAIL, HUESPED_PASSWORD, "/profile");

        // ===== Paso 2: revisa sus próximas reservas y encuentra la que aún no ha iniciado =====
        driver.get(FRONT + "/my-reservations");
        long reservaId = idDeReservaConHabitacion(HABITACION);
        assertThat(texto(By.id("mi-reserva-estado-" + reservaId)))
                .as("la reserva que el huésped hizo tiempo atrás sigue sin iniciar")
                .isEqualTo("CONFIRMADA");

        // ===== Paso 3: en otra pestaña entra el operador =====
        // La sesión vive en sessionStorage (ver auth.service.ts), que es propio de cada pestaña:
        // por eso las dos sesiones conviven sin pisarse. El paso 7 lo vuelve a comprobar.
        driver.switchTo().newWindow(WindowType.TAB);
        String pestanaOperador = driver.getWindowHandle();
        iniciarSesion(OPERADOR_EMAIL, OPERADOR_PASSWORD, "/operator/reservas");

        // ===== Paso 4: va al perfil de reservas y abre la del huésped =====
        clic(FILTRO_CONFIRMADA);
        clic(By.id("operador-gestionar-" + reservaId));
        wait.until(ExpectedConditions.urlContains("/operator/reservas/" + reservaId));

        // La cuenta ya existe (nació al confirmarse la reserva) y trae cargos previos sin pagar:
        // se lee el saldo de partida en vez de suponerlo, así el monto esperado no queda quemado.
        esperarVisible(CUENTA_SALDO);
        BigDecimal saldoInicial = monto(CUENTA_SALDO);
        assertThat(texto(CUENTA_ESTADO)).isEqualTo("ABIERTA");

        // ===== Paso 5: el operador activa la reserva (check-in) =====
        clic(ACCION_CHECKIN);
        wait.until(condicion -> texto(RESERVA_ESTADO_DETALLE).equals("EN_CURSO"));
        assertThat(texto(OPERADOR_MENSAJE)).isEqualTo("Check-in completed. The guest account is open.");
        assertThat(texto(CUENTA_ESTADO)).isEqualTo("ABIERTA");

        // ===== Paso 6: le agrega 2 servicios a la reserva =====
        int cargosPrevios = driver.findElements(CONSUMO_FILA).size();

        cargarServicio(SERVICIO_UNO, CANTIDAD_UNO, cargosPrevios + 1);
        BigDecimal esperadoTrasUno = saldoInicial.add(precioUno.multiply(BigDecimal.valueOf(CANTIDAD_UNO)));
        assertThat(monto(CUENTA_SALDO)).isEqualByComparingTo(esperadoTrasUno);

        cargarServicio(SERVICIO_DOS, CANTIDAD_DOS, cargosPrevios + 2);
        BigDecimal totalEsperado = esperadoTrasUno.add(precioDos.multiply(BigDecimal.valueOf(CANTIDAD_DOS)));

        // Los cargos de la cuenta son el saldo previo más los dos servicios recién agregados
        assertThat(monto(CUENTA_TOTAL_CONSUMOS)).isEqualByComparingTo(totalEsperado);
        assertThat(monto(CUENTA_SALDO)).isEqualByComparingTo(totalEsperado);

        // ===== Paso 7: el huésped, en su pestaña, ve que su estadía ya empezó =====
        driver.switchTo().window(pestanaHuesped);
        driver.get(FRONT + "/my-reservations");
        assertThat(driver.getCurrentUrl())
                .as("la sesión del huésped sigue viva: sessionStorage es propio de cada pestaña")
                .contains("/my-reservations");
        wait.until(condicion -> texto(By.id("mi-reserva-estado-" + reservaId)).equals("EN_CURSO"));

        // ===== Paso 8: termina la estadía; el operador cobra todo el saldo pendiente =====
        driver.switchTo().window(pestanaOperador);

        // "Pay full balance" copia el saldo al campo de monto: es donde se comprueba que lo que se
        // va a cobrar es exactamente lo que se debe, antes de registrar el pago.
        clic(PAGO_SALDO_COMPLETO);
        wait.until(condicion -> !valor(PAGO_MONTO).isEmpty());
        assertThat(new BigDecimal(valor(PAGO_MONTO)))
                .as("el monto a cobrar es el saldo pendiente completo")
                .isEqualByComparingTo(totalEsperado);

        new Select(esperarVisible(PAGO_METODO)).selectByValue(METODO_PAGO);
        clic(PAGO_REGISTRAR);

        // ===== Paso 9: la cuenta queda saldada y todos los cargos marcados como pagados =====
        wait.until(condicion -> monto(CUENTA_SALDO).signum() == 0);
        assertThat(texto(OPERADOR_MENSAJE)).isEqualTo("Payment registered.");
        assertThat(monto(CUENTA_TOTAL_PAGADO)).isEqualByComparingTo(totalEsperado);
        esperarVisible(CUENTA_SALDADA);
        assertThat(driver.findElements(CONSUMO_PAGADO))
                .as("al saldar la cuenta el backend marca pagados todos los cargos")
                .hasSize(cargosPrevios + 2);

        // ===== Paso 10: con la cuenta al día, el check-out cierra la estadía =====
        clic(ACCION_CHECKOUT);
        wait.until(condicion -> texto(RESERVA_ESTADO_DETALLE).equals("FINALIZADA"));
        assertThat(texto(OPERADOR_MENSAJE)).isEqualTo("Check-out completed. The room is available again.");
        assertThat(texto(CUENTA_ESTADO)).isEqualTo("CERRADA");
        // Con la cuenta cerrada desaparecen los formularios de cargo y de pago
        assertThat(driver.findElements(PAGO_REGISTRAR)).isEmpty();
        assertThat(driver.findElements(CONSUMO_AGREGAR)).isEmpty();

        // ===== Paso 11: el estado final del sistema, comprobado contra la base =====
        assertThat(reservaService.obtenerPorId(reservaId).getEstado()).isEqualTo(EstadoReserva.FINALIZADA);

        CuentaResponse cuentaFinal = cuentaService.obtenerRespuestaPorReserva(reservaId);
        assertThat(cuentaFinal.estado()).isEqualTo(EstadoCuenta.CERRADA);
        assertThat(cuentaFinal.saldo()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(cuentaFinal.totalPagado()).isEqualByComparingTo(totalEsperado);
        assertThat(cuentaFinal.items()).allMatch(CuentaResponse.ItemResponse::pagado);

        // La habitación se libera al hacer el check-out y vuelve a estar disponible
        Habitacion habitacion = habitacionRepository.findByNumero(HABITACION).orElseThrow();
        assertThat(habitacion.getEstado()).isEqualTo(EstadoHabitacion.DISPONIBLE);

        // ===== Paso 12: para el huésped la reserva pasó de sus reservas activas a su historial =====
        driver.switchTo().window(pestanaHuesped);
        driver.get(FRONT + "/my-reservations");
        esperarVisible(MIS_RESERVAS_TAB_HISTORIAL);
        wait.until(condicion -> driver.findElements(By.id("mi-reserva-" + reservaId)).isEmpty());

        clic(MIS_RESERVAS_TAB_HISTORIAL);
        esperarVisible(By.id("mi-reserva-" + reservaId));
        assertThat(texto(By.id("mi-reserva-estado-" + reservaId))).isEqualTo("FINALIZADA");
    }

    // ===== Inicia sesión y espera a que el rol aterrice en su propio panel =====
    private void iniciarSesion(String email, String password, String destinoEsperado) {
        driver.get(FRONT + "/login");
        escribir(LOGIN_EMAIL, email);
        escribir(LOGIN_PASSWORD, password);
        clic(LOGIN_ENVIAR);
        wait.until(ExpectedConditions.urlContains(destinoEsperado));
    }

    // ===== Busca en "My Reservations" la tarjeta de una habitación y devuelve el id de su reserva =====
    // El id se lee de la propia pantalla (el atributo id de la tarjeta), no se escribe a mano: los
    // ids los genera la base al arrancar y cambiarían si el DataLoader siembra en otro orden.
    private long idDeReservaConHabitacion(String numeroHabitacion) {
        esperarVisible(MI_RESERVA_ITEM);
        for (WebElement tarjeta : driver.findElements(MI_RESERVA_ITEM)) {
            String habitacion = tarjeta.findElement(By.className("tarjeta-admin-descripcion"))
                    .getAttribute("textContent").trim();
            if (habitacion.equals("Room " + numeroHabitacion)) {
                return Long.parseLong(tarjeta.getAttribute("id").replace("mi-reserva-", ""));
            }
        }
        throw new AssertionError("El huésped no tiene ninguna reserva activa de la habitación " + numeroHabitacion);
    }

    // ===== Carga un servicio a la cuenta y espera a que aparezca su fila =====
    private void cargarServicio(String nombreServicio, int cantidad, int filasEsperadas) {
        seleccionarServicio(nombreServicio);
        escribir(CONSUMO_CANTIDAD, String.valueOf(cantidad));
        clic(CONSUMO_AGREGAR);

        // La fila aparece cuando el backend responde con la cuenta actualizada
        wait.until(condicion -> driver.findElements(CONSUMO_FILA).size() == filasEsperadas);
        assertThat(texto(OPERADOR_MENSAJE)).isEqualTo("Charge added to the account.");
    }

    // ===== Elige un servicio del desplegable por su nombre =====
    // Las <option> las pinta Angular con [ngValue], así que su atributo value es interno
    // ("1: 1") y selectByValue no sirve: se localiza la opción por el texto que se ve.
    private void seleccionarServicio(String nombreServicio) {
        Select selector = new Select(esperarVisible(CONSUMO_SERVICIO));
        List<WebElement> opciones = selector.getOptions();
        for (int i = 0; i < opciones.size(); i++) {
            if (opciones.get(i).getAttribute("textContent").trim().startsWith(nombreServicio)) {
                selector.selectByIndex(i);
                return;
            }
        }
        throw new AssertionError("El desplegable no ofrece el servicio " + nombreServicio);
    }

    private BigDecimal precioServicio(String nombreServicio) {
        return BigDecimal.valueOf(servicioRepository.findByNombreIgnoreCase(nombreServicio)
                .orElseThrow(() -> new AssertionError("El DataLoader no sembró el servicio " + nombreServicio))
                .getPrecio());
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

    private String valor(By localizador) {
        String valor = esperarVisible(localizador).getAttribute("value");
        return valor == null ? "" : valor.trim();
    }

    // ===== Lee un importe de la pantalla como número =====
    // Se quitan el símbolo y los separadores de miles para no depender del formato de currency:'USD'
    private BigDecimal monto(By localizador) {
        String limpio = texto(localizador).replaceAll("[^0-9.-]", "");
        return new BigDecimal(limpio);
    }
}
