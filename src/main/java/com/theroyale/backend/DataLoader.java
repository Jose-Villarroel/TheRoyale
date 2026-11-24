package com.theroyale.backend;

import com.theroyale.backend.dto.plan.EntendidoItem;
import com.theroyale.backend.dto.plan.PreferenciasPlan;
import com.theroyale.backend.model.Admin;
import com.theroyale.backend.model.CategoriaLugar;
import com.theroyale.backend.model.Interes;
import com.theroyale.backend.model.LugarNyc;
import com.theroyale.backend.model.Presupuesto;
import com.theroyale.backend.model.Ritmo;
import com.theroyale.backend.model.Cliente;
import com.theroyale.backend.model.Cuenta;
import com.theroyale.backend.model.EstadoCuenta;
import com.theroyale.backend.model.EstadoHabitacion;
import com.theroyale.backend.model.EstadoReserva;
import com.theroyale.backend.model.Habitacion;
import com.theroyale.backend.model.ItemConsumo;
import com.theroyale.backend.model.Operador;
import com.theroyale.backend.model.Pago;
import com.theroyale.backend.model.CategoriaPlato;
import com.theroyale.backend.model.EtiquetaPlato;
import com.theroyale.backend.model.PlatoMenu;
import com.theroyale.backend.model.RestauranteHotel;
import com.theroyale.backend.model.Resena;
import com.theroyale.backend.model.Reserva;
import com.theroyale.backend.model.Servicio;
import com.theroyale.backend.model.TipoHabitacion;
import com.theroyale.backend.model.TipoOperador;
import com.theroyale.backend.model.Role;
import com.theroyale.backend.model.UserEntity;
import com.theroyale.backend.repository.AdminRepository;
import com.theroyale.backend.repository.ClienteRepository;
import com.theroyale.backend.repository.CuentaRepository;
import com.theroyale.backend.repository.HabitacionRepository;
import com.theroyale.backend.repository.LugarNycRepository;
import com.theroyale.backend.repository.OperadorRepository;
import com.theroyale.backend.repository.PlatoMenuRepository;
import com.theroyale.backend.repository.ResenaRepository;
import com.theroyale.backend.repository.ReservaRepository;
import com.theroyale.backend.repository.RoleRepository;
import com.theroyale.backend.repository.ServicioRepository;
import com.theroyale.backend.repository.TipoHabitacionRepository;
import com.theroyale.backend.repository.UserEntityRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.theroyale.backend.service.plan.PlanGenerado;
import com.theroyale.backend.service.plan.PlanGenerado.BloqueGenerado;
import com.theroyale.backend.service.plan.PlanGenerado.DiaGenerado;
import com.theroyale.backend.service.plan.PlanVisitaService;
import com.theroyale.backend.model.Temporada;
import com.theroyale.backend.repository.TemporadaRepository;
import com.theroyale.backend.service.ocupacion.HistoricoOcupacionService;
import com.theroyale.backend.service.ocupacion.PronosticoOcupacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

// ===== Carga datos iniciales de prueba cada vez que la aplicación arranca =====
// Necesario porque ddl-auto=create-drop borra y recrea la base de datos en cada reinicio.
// Nota: este es el único archivo del proyecto donde se accede directo a los Repository,
// saltándose la capa de Service — es una excepción intencional solo para este propósito.
// Solo corre en el perfil por defecto: las pruebas (perfil "test") preparan sus propios datos.
@Component
@Profile("default")
@Transactional
public class DataLoader implements CommandLineRunner {

    @Autowired
    private TipoHabitacionRepository tipoHabitacionRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private HabitacionRepository habitacionRepository;

    @Autowired
    private ServicioRepository servicioRepository;

    @Autowired
    private OperadorRepository operadorRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private CuentaRepository cuentaRepository;

    @Autowired
    private ResenaRepository resenaRepository;

    @Autowired
    private PlatoMenuRepository platoMenuRepository;

    @Autowired
    private LugarNycRepository lugarNycRepository;

    @Autowired
    private PlanVisitaService planVisitaService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserEntityRepository userEntityRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TemporadaRepository temporadaRepository;

    @Autowired
    private HistoricoOcupacionService historicoOcupacionService;

    private Role rolAdmin;
    private Role rolOperador;
    private Role rolCliente;


    // ===== Versiones en español del contenido de la base que ve el huésped =====
    // El inglés es el idioma base; estas tablas son lo que guardan nombreEs / descripcionEs. Van
    // juntas aquí (y no intercaladas en los bloques de arriba) para poder revisar la traducción de
    // un tirón, y se aplican al insertar: una pasada posterior con save() haría merge sobre una
    // entidad desacoplada y Hibernate no puede reemplazar las listas inmutables (List.of) de
    // caracteristicas y galeriaUrls. Hoy esta es la única forma de cargar estos valores: el panel
    // de administración todavía no tiene formulario para ellos.
    //
    // Reglas de docs/i18n-glosario.md: la marca "The Royale", los nombres de tipo de habitación y
    // de servicio tal como están en la base, los nombres de plato que son nombres propios de cocina
    // ("Eggs Benedict", "Buddha Bowl", "The Royale Manhattan") y los topónimos NO se traducen. Lo
    // que no está en la tabla queda en null y el DTO de respuesta cae al inglés (TextoPorIdioma).

    // Servicios: el nombre es el de la base ("Wellness", "Dining"...), así que solo la descripción
    private static final Map<String, String> SERVICIOS_DESCRIPCION_ES = Map.of(
            "Wellness", "Sumérjase en una relajación total en The Royale Spa. Nuestro santuario de bienestar está diseñado alrededor de su descanso: de los rituales termales de siempre al fitness más moderno, cada detalle está pensado para el huésped más exigente.",
            "Dining", "Disfrute de una cocina excepcional sin salir de The Royale. Nuestro restaurante de autor sirve cocina neoyorquina contemporánea desde el desayuno hasta la cena. El servicio a la habitación está disponible las 24 horas.",
            "Business", "Siga siendo productivo en el corazón de Manhattan. The Royale Business Center ofrece todo lo que necesita un huésped corporativo, de la tecnología de reuniones más avanzada al apoyo ejecutivo completo, a pocos pasos de su habitación.",
            "Concierge", "Nuestro equipo de Concierge multilingüe está disponible a toda hora para orquestar cada detalle de su experiencia en New York. Ninguna solicitud es demasiado extraordinaria: nos especializamos en convertir lo imposible en inolvidable.",
            "Transportation", "Recorra New York con la misma comodidad y discreción que espera dentro de The Royale. Nuestro servicio de transporte coordina traslados privados, vehículos con chofer y rutas por la ciudad para huéspedes que valoran la puntualidad, el confort y la privacidad.");

    // Platos: solo los que no son nombres propios de cocina llevan nombre en español
    private static final Map<String, String> PLATOS_NOMBRE_ES = Map.ofEntries(
            Map.entry("Yellowfin Tuna Tartare", "Tartar de atún aleta amarilla"),
            Map.entry("Pan-Seared Atlantic Salmon", "Salmón del Atlántico sellado a la sartén"),
            Map.entry("Wild Mushroom Risotto", "Risotto de hongos silvestres"),
            Map.entry("Spiced Cauliflower Steak", "Coliflor asada especiada"),
            Map.entry("Maine Lobster Linguine", "Linguine con langosta de Maine"),
            Map.entry("Dark Chocolate Mousse", "Mousse de chocolate amargo"),
            Map.entry("Truffle Parmesan Fries", "Papas fritas con trufa y parmesano"),
            Map.entry("Classic Cheeseburger", "Hamburguesa clásica con queso"),
            Map.entry("Chicken Caesar Salad", "Ensalada César con pollo"),
            Map.entry("Seasonal Fruit Platter", "Tabla de frutas de temporada"),
            Map.entry("Buttermilk Pancakes", "Panqueques de buttermilk"),
            Map.entry("Smoked Salmon Bagel", "Bagel con salmón ahumado"),
            Map.entry("Greek Yogurt Parfait", "Parfait de yogur griego"),
            Map.entry("Cold-Pressed Green Juice", "Jugo verde prensado en frío"));

    private static final Map<String, String> PLATOS_DESCRIPCION_ES = Map.ofEntries(
            Map.entry("Burrata and Heirloom Tomatoes", "Burrata cremosa, tomates heirloom del Hudson Valley, aceite de albahaca y sal marina."),
            Map.entry("Yellowfin Tuna Tartare", "Atún aleta amarilla cortado a mano, palta, aderezo de yuzu y crocante de sésamo."),
            Map.entry("Dry-Aged New York Strip", "Bife de chorizo madurado 28 días en seco, mantequilla de tuétano asado y puré de papa."),
            Map.entry("Pan-Seared Atlantic Salmon", "Salmón de piel crocante, beurre blanc de limón, espárragos asados y papas fingerling."),
            Map.entry("Wild Mushroom Risotto", "Arroz carnaroli, hongos de bosque, parmesano añejo y aceite de trufa blanca."),
            Map.entry("Spiced Cauliflower Steak", "Coliflor asada con harissa, tahini, granada y quinua con hierbas."),
            Map.entry("Maine Lobster Linguine", "Linguine fresco, langosta de Maine pochada en mantequilla, tomates cherry y chile. De temporada."),
            Map.entry("Classic New York Cheesecake", "Cheesecake de vainilla sobre base de galleta graham con compota de frutos rojos."),
            Map.entry("Dark Chocolate Mousse", "Mousse sedosa de chocolate amargo 70 % y coco con frambuesas."),
            Map.entry("The Royale Manhattan", "Whiskey de centeno, vermut dulce, bitters de la casa y cereza al brandy, batido al momento."),
            Map.entry("Fifth Avenue Spritz", "Licor de flor de saúco, prosecco, durazno blanco y soda."),
            Map.entry("Smoky Jalapeno Margarita", "Mezcal, limón fresco, agave y jalapeño con borde de sal y chile."),
            Map.entry("Garden Mocktail", "Mezcla sin alcohol de pepino, menta, limón y tónica."),
            Map.entry("Truffle Parmesan Fries", "Papas cortadas a mano, trufa negra, parmesano y alioli de romero."),
            Map.entry("Wagyu Sliders", "Tres mini hamburguesas de wagyu con cheddar añejo y cebolla caramelizada."),
            Map.entry("The Royale Club Sandwich", "Pavo asado, tocino, palta, tomate y mayonesa de hierbas en brioche tostado, con papas fritas."),
            Map.entry("Classic Cheeseburger", "Medallón de res prime, cheddar, lechuga, tomate y salsa de la casa, con papas fritas."),
            Map.entry("Buddha Bowl", "Quinua, camote asado, garbanzos, kale, palta y aderezo de miso y jengibre."),
            Map.entry("Chicken Caesar Salad", "Pollo a la parrilla, corazones de lechuga romana, parmesano, croutones y aderezo César clásico."),
            Map.entry("Seasonal Fruit Platter", "Selección de frutas frescas de temporada con menta."),
            Map.entry("Eggs Benedict", "Huevos pochados, jamón ahumado y holandesa sobre un English muffin tostado."),
            Map.entry("Avocado Toast", "Palta machacada, tomates cherry, hojuelas de chile y brotes sobre pan de masa madre."),
            Map.entry("Buttermilk Pancakes", "Torre esponjosa con jarabe de maple de Vermont, frutos rojos y mantequilla batida."),
            Map.entry("Acai Bowl", "Açaí, banano, crocante de semillas tostadas, frutos rojos frescos y coco."),
            Map.entry("Smoked Salmon Bagel", "Bagel neoyorquino, salmón ahumado, queso crema, alcaparras y cebolla morada."),
            Map.entry("Greek Yogurt Parfait", "Yogur griego, granola sin gluten, miel y frutos rojos de temporada."),
            Map.entry("Cold-Pressed Green Juice", "Kale, pepino, manzana verde, apio, limón y jengibre."));

    private void cargarRoles() {
        rolAdmin = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN")));
        rolOperador = roleRepository.findByName("ROLE_OPERADOR")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_OPERADOR")));
        rolCliente = roleRepository.findByName("ROLE_CLIENTE")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_CLIENTE")));
    }

    private UserEntity registrarUsuario(String username, String rawPassword, Role rol) {
        String pass = (rawPassword != null && !rawPassword.isBlank()) ? rawPassword : "123";
        return userEntityRepository.findByUsername(username)
                .orElseGet(() -> userEntityRepository.save(UserEntity.builder()
                        .username(username)
                        .password(passwordEncoder.encode(pass))
                        .roles(new ArrayList<>(List.of(rol)))
                        .build()));
    }

    @Override
    public void run(String... args) throws Exception {
        cargarRoles();
        cargarTiposHabitacion();
        cargarClientes();
        cargarHabitaciones();
        cargarServicios();
        cargarOperadores();
        cargarAdmins();
        cargarReservas();
        cargarReservasParaPlan();
        cargarPlatosMenu();
        cargarLugaresNyc();
        cargarPlanDeEjemplo();
        cargarTemporadas();
        // Historial simulado para el pronóstico de ocupación (ver Docs/modelo/README.md)
        historicoOcupacionService.cargar(LocalDate.now(PronosticoOcupacionService.ZONA_NUEVA_YORK));
        // Al final a proposito: solo lee lo ya sembrado y no mueve ningun indice
        cargarHistorialDashboard();
    }

    // ===== Temporadas de precios iniciales, para este año y el siguiente =====
    // Solo alimentan las recomendaciones: el admin decide si aplica el precio que se le propone.
    private void cargarTemporadas() {
        int anio = LocalDate.now(PronosticoOcupacionService.ZONA_NUEVA_YORK).getYear();
        for (int a = anio; a <= anio + 1; a++) {
            guardarTemporada("Low season", LocalDate.of(a, 1, 3), LocalDate.of(a, 3, 15), 0.85);
            guardarTemporada("Summer", LocalDate.of(a, 6, 1), LocalDate.of(a, 8, 31), 1.15);
            guardarTemporada("Holiday season", LocalDate.of(a, 12, 15), LocalDate.of(a + 1, 1, 2), 1.35);
        }
    }

    private void guardarTemporada(String nombre, LocalDate inicio, LocalDate fin, double factor) {
        temporadaRepository.save(Temporada.builder()
                .nombre(nombre).fechaInicio(inicio).fechaFin(fin).factor(factor).build());
    }

    // ===== Catálogo curado de 25 lugares de Nueva York para el plan de visita =====
    // Coordenadas reales; bajoTecho decide si el plan los evita con lluvia. Horarios de referencia.
    private void cargarLugaresNyc() {
        guardarLugar("Central Park", CategoriaLugar.PARQUE, 40.7829, -73.9654, false, 120,
                "Daily 06:00-01:00", "https://www.centralparknyc.org");
        guardarLugar("The Metropolitan Museum of Art", CategoriaLugar.MUSEO, 40.7794, -73.9632, true, 180,
                "Sun-Tue and Thu 10:00-17:00, Fri-Sat 10:00-21:00, closed Wednesday", "https://www.metmuseum.org");
        guardarLugar("The Museum of Modern Art (MoMA)", CategoriaLugar.MUSEO, 40.7614, -73.9776, true, 150,
                "Daily 10:30-17:30, Saturday until 19:00", "https://www.moma.org");
        guardarLugar("Top of the Rock", CategoriaLugar.MIRADOR, 40.7593, -73.9794, false, 60,
                "Daily 09:00-24:00", "https://www.rockefellercenter.com/attractions/top-of-the-rock-observation-deck/");
        guardarLugar("Rockefeller Center", CategoriaLugar.LANDMARK, 40.7587, -73.9787, false, 60,
                "Open daily; ice rink and Christmas tree in winter", "https://www.rockefellercenter.com");
        guardarLugar("St. Patrick's Cathedral", CategoriaLugar.LANDMARK, 40.7585, -73.9760, true, 45,
                "Daily 06:30-20:45", "https://saintpatrickscathedral.org");
        guardarLugar("Bryant Park", CategoriaLugar.PARQUE, 40.7536, -73.9832, false, 60,
                "Daily 07:00-22:00; Winter Village in season", "https://bryantpark.org");
        guardarLugar("New York Public Library (Stephen A. Schwarzman Building)", CategoriaLugar.LANDMARK, 40.7532, -73.9822, true, 60,
                "Mon-Sat 10:00-18:00 (Tue-Wed until 20:00), Sun 13:00-17:00", "https://www.nypl.org/locations/schwarzman");
        guardarLugar("Grand Central Terminal", CategoriaLugar.LANDMARK, 40.7527, -73.9772, true, 45,
                "Daily 05:15-02:00", "https://www.grandcentralterminal.com");
        guardarLugar("Times Square", CategoriaLugar.LANDMARK, 40.7580, -73.9855, false, 45,
                "Always open; best after dark", "https://www.timessquarenyc.org");
        guardarLugar("The High Line", CategoriaLugar.PASEO, 40.7480, -74.0048, false, 90,
                "Daily 07:00-22:00 (until 19:00 in winter)", "https://www.thehighline.org");
        guardarLugar("Vessel and Hudson Yards", CategoriaLugar.LANDMARK, 40.7538, -74.0022, false, 60,
                "Daily 10:00-21:00", "https://www.hudsonyardsnewyork.com");
        guardarLugar("Chelsea Market", CategoriaLugar.COMPRAS, 40.7424, -74.0061, true, 75,
                "Daily 07:00-02:00", "https://www.chelseamarket.com");
        guardarLugar("9/11 Memorial & Museum", CategoriaLugar.MUSEO, 40.7115, -74.0134, true, 150,
                "Museum Wed-Mon 09:00-19:00; Memorial plaza daily", "https://www.911memorial.org");
        guardarLugar("One World Observatory", CategoriaLugar.MIRADOR, 40.7130, -74.0132, true, 75,
                "Daily 09:00-21:00", "https://www.oneworldobservatory.com");
        guardarLugar("Brooklyn Bridge", CategoriaLugar.PASEO, 40.7061, -73.9969, false, 60,
                "Always open; walk from City Hall", "https://www.nyc.gov/html/dot/html/infrastructure/brooklyn-bridge.shtml");
        guardarLugar("DUMBO", CategoriaLugar.PASEO, 40.7033, -73.9881, false, 90,
                "Always open; Washington Street view of the Manhattan Bridge", "https://dumbo.is");
        guardarLugar("Statue of Liberty Ferry", CategoriaLugar.LANDMARK, 40.7033, -74.0170, false, 240,
                "Ferries daily from about 09:00 at Battery Park", "https://www.nps.gov/stli");
        guardarLugar("American Museum of Natural History", CategoriaLugar.MUSEO, 40.7813, -73.9740, true, 180,
                "Daily 10:00-17:30", "https://www.amnh.org");
        guardarLugar("Solomon R. Guggenheim Museum", CategoriaLugar.MUSEO, 40.7830, -73.9590, true, 120,
                "Sun-Mon and Wed-Fri 11:00-18:00, Sat until 20:00, closed Tuesday", "https://www.guggenheim.org");
        guardarLugar("Fifth Avenue shopping", CategoriaLugar.COMPRAS, 40.7625, -73.9739, false, 120,
                "Most stores 10:00-20:00", "https://www.fifthavenue.nyc");
        guardarLugar("Greenwich Village", CategoriaLugar.PASEO, 40.7336, -74.0027, false, 120,
                "Always open; Washington Square Park and Bleecker Street", "https://www.villagepreservation.org");
        guardarLugar("SoHo", CategoriaLugar.COMPRAS, 40.7233, -74.0030, false, 120,
                "Most stores 11:00-19:00", "https://sohobroadway.org");
        guardarLugar("Little Island", CategoriaLugar.PARQUE, 40.7420, -74.0103, false, 45,
                "Daily 06:00-01:00", "https://littleisland.org");
        guardarLugar("SUMMIT One Vanderbilt", CategoriaLugar.MIRADOR, 40.7531, -73.9787, true, 90,
                "Daily 09:00-24:00", "https://summitov.com");
    }

    private void guardarLugar(String nombre, CategoriaLugar categoria, double latitud, double longitud,
                              boolean bajoTecho, int duracionMinutos, String horario, String url) {
        lugarNycRepository.save(LugarNyc.builder()
                .nombre(nombre).categoria(categoria).latitud(latitud).longitud(longitud)
                .bajoTecho(bajoTecho).duracionMinutos(duracionMinutos).horarioReferencia(horario)
                .urlOficial(url).activo(true)
                .build());
    }

    // ===== Plan de visita de ejemplo, ya validado, para que la demo funcione sin claves =====
    // Reserva CONFIRMADA de la 703 (Sofia Torres, sofia.torres@email.com / pass1234): llega en 10 días,
    // 4 noches, 3 huéspedes. No la usa ninguna prueba E2E. Se escribe como lo devolvería Gemini y pasa
    // por ValidadorPlanService, así los nombres, URL, traslados y precios salen de las fuentes reales.
    private void cargarPlanDeEjemplo() {
        Reserva reserva = reservaRepository.findFirstByHabitacionNumeroAndEstadoIn("703", List.of(EstadoReserva.CONFIRMADA))
                .orElseThrow();
        LocalDate dia1 = reserva.getFechaInicio();
        Servicio wellness = servicioRepository.findByNombreIgnoreCase("Wellness").orElseThrow();
        Servicio concierge = servicioRepository.findByNombreIgnoreCase("Concierge").orElseThrow();

        PreferenciasPlan preferencias = new PreferenciasPlan(List.of(Interes.MUSEOS, Interes.PARQUES, Interes.FAMILIA),
                Ritmo.EQUILIBRADO, Presupuesto.ALTO, List.of(), false, true,
                "Our daughter loves dinosaurs. One special dinner, please.");
        String descripcion = "We are a family of three. Our daughter loves dinosaurs, we like parks and calm "
                + "mornings, and we would like one special dinner during the trip.";
        List<EntendidoItem> entendido = List.of(
                new EntendidoItem("A family trip.", "Every day has family-friendly places and time to rest."),
                new EntendidoItem("Dinosaurs first.", "Your first full morning is at the American Museum of Natural History."),
                new EntendidoItem("One special dinner.", "Your arrival evening ends at the Signature Restaurant."));

        PlanGenerado plan = new PlanGenerado(
                "A gentle family week: dinosaurs, Central Park, the city from above and one special dinner.",
                List.of(
                        new DiaGenerado(dia1.toString(), "An easy first evening", List.of(
                                bloque("16:00", "17:30", "LUGAR", lugarId("Rockefeller Center"), null,
                                        "A short walk from the hotel to see the plaza and the rink."),
                                bloque("19:00", "20:30", "RESTAURANTE_HOTEL", "SIGNATURE_RESTAURANT", null,
                                        "Your special dinner, without leaving the hotel."))),
                        new DiaGenerado(dia1.plusDays(1).toString(), "Dinosaurs and Central Park", List.of(
                                bloque("08:00", "09:00", "RESTAURANTE_HOTEL", "BREAKFAST_LOUNGE", null,
                                        "A calm breakfast before the museum."),
                                bloque("10:00", "13:00", "LUGAR", lugarId("American Museum of Natural History"), null,
                                        "The dinosaur halls your daughter is waiting for."),
                                bloque("13:00", "14:00", "TIEMPO_LIBRE", "", null,
                                        "Lunch is yours to choose on Columbus Avenue, next to the museum."),
                                bloque("14:30", "16:30", "LUGAR", lugarId("Central Park"), null,
                                        "Cross the park on foot back toward the hotel."),
                                bloque("17:30", "18:30", "SERVICIO_HOTEL", String.valueOf(wellness.getId()),
                                        "Heated Indoor Pool - Open daily 06:00 to 22:00",
                                        "A swim for everyone after a long day outside."))),
                        new DiaGenerado(dia1.plusDays(2).toString(), "Art and the city from above", List.of(
                                bloque("10:00", "12:30", "LUGAR", lugarId("The Metropolitan Museum of Art"), null,
                                        "Armor, mummies and the Temple of Dendur hold a child's attention."),
                                bloque("13:00", "14:00", "RESTAURANTE_HOTEL", "SIGNATURE_RESTAURANT", null,
                                        "Lunch back at the hotel to rest a little."),
                                bloque("15:30", "16:30", "LUGAR", lugarId("Top of the Rock"), null,
                                        "Central Park and the whole skyline from the terraces."))),
                        new DiaGenerado(dia1.plusDays(3).toString(), "The West Side on foot", List.of(
                                bloque("10:00", "11:30", "LUGAR", lugarId("The High Line"), null,
                                        "An elevated park walk with gardens and city views."),
                                bloque("11:45", "13:00", "LUGAR", lugarId("Chelsea Market"), null,
                                        "Many food stalls, so everyone picks their own lunch."),
                                bloque("16:00", "17:30", "SERVICIO_HOTEL", String.valueOf(concierge.getId()),
                                        "Private Museum Tours - After-hours access to MET, MoMA and more",
                                        "A quiet private visit, with no crowds."),
                                bloque("19:00", "20:00", "RESTAURANTE_HOTEL", "ROOM_SERVICE", null,
                                        "Dinner in the suite after a full day."))),
                        new DiaGenerado(dia1.plusDays(4).toString(), "A last stroll", List.of(
                                bloque("08:00", "09:00", "RESTAURANTE_HOTEL", "BREAKFAST_LOUNGE", null,
                                        "A last breakfast at the hotel."),
                                bloque("09:30", "11:00", "LUGAR", lugarId("Bryant Park"), null,
                                        "A short walk before check-out at noon.")))));

        planVisitaService.sembrarPlanDeEjemplo(reserva, preferencias, descripcion, entendido, plan);
    }

    private BloqueGenerado bloque(String inicio, String fin, String tipo, String referenciaId, String caracteristica,
                                  String motivo) {
        return new BloqueGenerado(inicio, fin, tipo, referenciaId, caracteristica, "", motivo);
    }

    private String lugarId(String nombre) {
        return String.valueOf(lugarNycRepository.findByNombre(nombre).orElseThrow().getId());
    }

    // ===== Carta de los 4 restaurantes (28 platos), coherente con las características de "Dining" =====
    // Incluye opciones veganas, vegetarianas y sin gluten ("Dietary Menus"). Uno queda no disponible
    // (plato de temporada) para que el admin y el filtro soloDisponibles tengan con qué trabajar.
    private void cargarPlatosMenu() {
        // Signature Restaurant: cocina contemporánea de Nueva York
        guardarPlato("Roasted Beet Carpaccio", "Thinly sliced roasted beets, citrus segments, pistachio crumble and aged balsamic.",
                "17.00",
                "/images/menu/roasted-beet-carpaccio.jpg", RestauranteHotel.SIGNATURE_RESTAURANT, CategoriaPlato.ENTRADA, true,
                EtiquetaPlato.VEGANO, EtiquetaPlato.SIN_GLUTEN);
        guardarPlato("Burrata and Heirloom Tomatoes", "Creamy burrata, heirloom tomatoes from the Hudson Valley, basil oil and sea salt.",
                "19.00",
                "/images/menu/burrata-heirloom-tomatoes.jpg", RestauranteHotel.SIGNATURE_RESTAURANT, CategoriaPlato.ENTRADA, true,
                EtiquetaPlato.VEGETARIANO, EtiquetaPlato.SIN_GLUTEN);
        guardarPlato("Yellowfin Tuna Tartare", "Hand-cut yellowfin tuna, avocado, yuzu dressing and sesame tuile.",
                "24.00",
                "/images/menu/yellowfin-tuna-tartare.jpg", RestauranteHotel.SIGNATURE_RESTAURANT, CategoriaPlato.ENTRADA, true);
        guardarPlato("Dry-Aged New York Strip", "28-day dry-aged strip steak, roasted bone marrow butter and pommes puree.",
                "58.00",
                "/images/menu/dry-aged-ny-strip.jpg", RestauranteHotel.SIGNATURE_RESTAURANT, CategoriaPlato.PRINCIPAL, true,
                EtiquetaPlato.SIN_GLUTEN);
        guardarPlato("Pan-Seared Atlantic Salmon", "Crispy-skin salmon, lemon beurre blanc, charred asparagus and fingerling potatoes.",
                "42.00",
                "/images/menu/pan-seared-salmon.jpg", RestauranteHotel.SIGNATURE_RESTAURANT, CategoriaPlato.PRINCIPAL, true,
                EtiquetaPlato.SIN_GLUTEN);
        guardarPlato("Wild Mushroom Risotto", "Carnaroli rice, forest mushrooms, aged parmesan and white truffle oil.",
                "34.00",
                "/images/menu/wild-mushroom-risotto.jpg", RestauranteHotel.SIGNATURE_RESTAURANT, CategoriaPlato.PRINCIPAL, true,
                EtiquetaPlato.VEGETARIANO, EtiquetaPlato.SIN_GLUTEN);
        guardarPlato("Spiced Cauliflower Steak", "Harissa-roasted cauliflower, tahini, pomegranate and herb quinoa.",
                "29.00",
                "/images/menu/spiced-cauliflower-steak.jpg", RestauranteHotel.SIGNATURE_RESTAURANT, CategoriaPlato.PRINCIPAL, true,
                EtiquetaPlato.VEGANO, EtiquetaPlato.SIN_GLUTEN, EtiquetaPlato.PICANTE);
        guardarPlato("Maine Lobster Linguine", "Fresh linguine, butter-poached Maine lobster, cherry tomatoes and chili. Seasonal.",
                "49.00",
                "/images/menu/maine-lobster-linguine.jpg", RestauranteHotel.SIGNATURE_RESTAURANT, CategoriaPlato.PRINCIPAL, false,
                EtiquetaPlato.PICANTE);
        guardarPlato("Classic New York Cheesecake", "Vanilla bean cheesecake on a graham cracker crust with berry compote.",
                "14.00",
                "/images/menu/ny-cheesecake.jpg", RestauranteHotel.SIGNATURE_RESTAURANT, CategoriaPlato.POSTRE, true,
                EtiquetaPlato.VEGETARIANO);
        guardarPlato("Dark Chocolate Mousse", "Silky 70% dark chocolate and coconut mousse with raspberries.",
                "13.00",
                "/images/menu/dark-chocolate-mousse.jpg", RestauranteHotel.SIGNATURE_RESTAURANT, CategoriaPlato.POSTRE, true,
                EtiquetaPlato.VEGANO, EtiquetaPlato.SIN_GLUTEN);

        // Craft Cocktail Bar: cócteles de autor y algo para picar
        guardarPlato("The Royale Manhattan", "Rye whiskey, sweet vermouth, house bitters and a brandied cherry, stirred to order.",
                "22.00",
                "/images/menu/royale-manhattan.jpg", RestauranteHotel.COCKTAIL_BAR, CategoriaPlato.BEBIDA, true,
                EtiquetaPlato.VEGANO, EtiquetaPlato.SIN_GLUTEN);
        guardarPlato("Fifth Avenue Spritz", "Elderflower liqueur, prosecco, white peach and soda.",
                "19.00",
                "/images/menu/fifth-avenue-spritz.jpg", RestauranteHotel.COCKTAIL_BAR, CategoriaPlato.BEBIDA, true,
                EtiquetaPlato.VEGANO, EtiquetaPlato.SIN_GLUTEN);
        guardarPlato("Smoky Jalapeno Margarita", "Mezcal, fresh lime, agave and jalapeno with a chili-salt rim.",
                "20.00",
                "/images/menu/smoky-jalapeno-margarita.jpg", RestauranteHotel.COCKTAIL_BAR, CategoriaPlato.BEBIDA, true,
                EtiquetaPlato.VEGANO, EtiquetaPlato.SIN_GLUTEN, EtiquetaPlato.PICANTE);
        guardarPlato("Garden Mocktail", "Alcohol-free blend of cucumber, mint, lime and sparkling tonic.",
                "12.00",
                "/images/menu/garden-mocktail.jpg", RestauranteHotel.COCKTAIL_BAR, CategoriaPlato.BEBIDA, true,
                EtiquetaPlato.VEGANO, EtiquetaPlato.SIN_GLUTEN);
        guardarPlato("Truffle Parmesan Fries", "Hand-cut fries, black truffle, parmesan and rosemary aioli.",
                "14.00",
                "/images/menu/truffle-parmesan-fries.jpg", RestauranteHotel.COCKTAIL_BAR, CategoriaPlato.ENTRADA, true,
                EtiquetaPlato.VEGETARIANO);
        guardarPlato("Wagyu Sliders", "Three mini wagyu burgers with aged cheddar and caramelized onions.",
                "24.00",
                "/images/menu/wagyu-sliders.jpg", RestauranteHotel.COCKTAIL_BAR, CategoriaPlato.ENTRADA, true);

        // 24h Room Service: carta a la habitación a cualquier hora
        guardarPlato("The Royale Club Sandwich", "Roasted turkey, bacon, avocado, tomato and herb mayo on toasted brioche, with fries.",
                "26.00",
                "/images/menu/royale-club-sandwich.jpg", RestauranteHotel.ROOM_SERVICE, CategoriaPlato.PRINCIPAL, true);
        guardarPlato("Classic Cheeseburger", "Prime beef patty, cheddar, lettuce, tomato and house sauce, with fries.",
                "28.00",
                "/images/menu/classic-cheeseburger.jpg", RestauranteHotel.ROOM_SERVICE, CategoriaPlato.PRINCIPAL, true);
        guardarPlato("Buddha Bowl", "Quinoa, roasted sweet potato, chickpeas, kale, avocado and miso-ginger dressing.",
                "24.00",
                "/images/menu/buddha-bowl.jpg", RestauranteHotel.ROOM_SERVICE, CategoriaPlato.PRINCIPAL, true,
                EtiquetaPlato.VEGANO, EtiquetaPlato.SIN_GLUTEN);
        guardarPlato("Chicken Caesar Salad", "Grilled chicken, romaine hearts, parmesan, croutons and classic Caesar dressing.",
                "22.00",
                "/images/menu/chicken-caesar-salad.jpg", RestauranteHotel.ROOM_SERVICE, CategoriaPlato.PRINCIPAL, true);
        guardarPlato("Seasonal Fruit Platter", "A selection of fresh seasonal fruit with mint.",
                "15.00",
                "/images/menu/seasonal-fruit-platter.jpg", RestauranteHotel.ROOM_SERVICE, CategoriaPlato.POSTRE, true,
                EtiquetaPlato.VEGANO, EtiquetaPlato.SIN_GLUTEN);

        // Breakfast Lounge
        guardarPlato("Eggs Benedict", "Poached eggs, smoked ham and hollandaise on a toasted English muffin.",
                "24.00",
                "/images/menu/eggs-benedict.jpg", RestauranteHotel.BREAKFAST_LOUNGE, CategoriaPlato.DESAYUNO, true);
        guardarPlato("Avocado Toast", "Smashed avocado, cherry tomatoes, chili flakes and microgreens on sourdough.",
                "19.00",
                "/images/menu/avocado-toast.jpg", RestauranteHotel.BREAKFAST_LOUNGE, CategoriaPlato.DESAYUNO, true,
                EtiquetaPlato.VEGANO);
        guardarPlato("Buttermilk Pancakes", "Fluffy stack with Vermont maple syrup, berries and whipped butter.",
                "18.00",
                "/images/menu/buttermilk-pancakes.jpg", RestauranteHotel.BREAKFAST_LOUNGE, CategoriaPlato.DESAYUNO, true,
                EtiquetaPlato.VEGETARIANO);
        guardarPlato("Acai Bowl", "Acai, banana, toasted seed crunch, fresh berries and coconut.",
                "16.00",
                "/images/menu/acai-bowl.jpg", RestauranteHotel.BREAKFAST_LOUNGE, CategoriaPlato.DESAYUNO, true,
                EtiquetaPlato.VEGANO, EtiquetaPlato.SIN_GLUTEN);
        guardarPlato("Smoked Salmon Bagel", "New York bagel, smoked salmon, cream cheese, capers and red onion.",
                "22.00",
                "/images/menu/smoked-salmon-bagel.jpg", RestauranteHotel.BREAKFAST_LOUNGE, CategoriaPlato.DESAYUNO, true);
        guardarPlato("Greek Yogurt Parfait", "Greek yogurt, gluten-free granola, honey and seasonal berries.",
                "14.00",
                "/images/menu/greek-yogurt-parfait.jpg", RestauranteHotel.BREAKFAST_LOUNGE, CategoriaPlato.DESAYUNO, true,
                EtiquetaPlato.VEGETARIANO, EtiquetaPlato.SIN_GLUTEN);
        guardarPlato("Cold-Pressed Green Juice", "Kale, cucumber, green apple, celery, lemon and ginger.",
                "11.00",
                "/images/menu/green-juice.jpg", RestauranteHotel.BREAKFAST_LOUNGE, CategoriaPlato.BEBIDA, true,
                EtiquetaPlato.VEGANO, EtiquetaPlato.SIN_GLUTEN);
    }

    private void guardarPlato(String nombre, String descripcion, String precio, String imagenUrl,
            RestauranteHotel restaurante, CategoriaPlato categoria, boolean disponible,
            EtiquetaPlato... etiquetas) {
        platoMenuRepository.save(PlatoMenu.builder()
                .nombre(nombre)
                .descripcion(descripcion)
                // null si el plato no esta en la tabla: el DTO cae al ingles (TextoPorIdioma)
                .nombreEs(PLATOS_NOMBRE_ES.get(nombre))
                .descripcionEs(PLATOS_DESCRIPCION_ES.get(nombre))
                .precio(new BigDecimal(precio))
                .restaurante(restaurante)
                .categoria(categoria)
                .disponible(disponible)
                .imagenUrl(imagenUrl)
                .etiquetas(new LinkedHashSet<>(List.of(etiquetas)))
                .build());
    }

    // ===== 2 administradores: gestionan el catálogo y el personal, no la operación diaria =====
    private void cargarAdmins() {
        Admin mono = Admin.builder()
                .nombre("Mono").email("mono@theroyale.com").password("admin123")
                .build();
        mono.setUser(registrarUsuario(mono.getEmail(), "admin123", rolAdmin));
        adminRepository.save(mono);

        Admin mati = Admin.builder()
                .nombre("Mati").email("mati@theroyale.com").password("admin123")
                .build();
        mati.setUser(registrarUsuario(mati.getEmail(), "admin123", rolAdmin));
        adminRepository.save(mati);
    }

    // ===== 5 tipos de habitación =====
    private void cargarTiposHabitacion() {
        tipoHabitacionRepository.save(TipoHabitacion.builder()
                .nombre("Normal")
                .descripcion("Elegant essentials for a refined stay. 1-2 guests, double bed, private bathroom, premium Wi-Fi.")
                // nombreEs se queda en null: los nombres de tipo de habitación no se traducen
                .descripcionEs("Lo esencial con elegancia para una estadía refinada. 1-2 huéspedes, cama doble, baño privado, Wi-Fi premium.")
                .imagenUrl("/images/hotel/hab-normal-4x3.jpg")
                .build());

        tipoHabitacionRepository.save(TipoHabitacion.builder()
                .nombre("Executive")
                .descripcion("Designed for those who work while they travel. 1-2 guests, executive workspace, king bed, premium Wi-Fi.")
                .descripcionEs("Pensada para quienes trabajan mientras viajan. 1-2 huéspedes, escritorio ejecutivo, cama king, Wi-Fi premium.")
                .imagenUrl("/images/hotel/hab-executive-4x3.jpg")
                .build());

        tipoHabitacionRepository.save(TipoHabitacion.builder()
                .nombre("VIP")
                .descripcion("A private space to relax and unwind. Up to 3 guests, separate living area, premium amenities, city views.")
                .descripcionEs("Un espacio privado para relajarse. Hasta 3 huéspedes, sala independiente, amenidades premium y vistas a la ciudad.")
                .imagenUrl("/images/hotel/hab-vip-4x3.jpg")
                .build());

        tipoHabitacionRepository.save(TipoHabitacion.builder()
                .nombre("Luxury")
                .descripcion("The ultimate expression of The Royale. Up to 4 guests, full suite, jacuzzi, privileged city view.")
                .descripcionEs("La máxima expresión de The Royale. Hasta 4 huéspedes, suite completa, jacuzzi y vista privilegiada de la ciudad.")
                .imagenUrl("/images/hotel/hab-luxury-4x3.jpg")
                .build());

        tipoHabitacionRepository.save(TipoHabitacion.builder()
                .nombre("Presidential Suite")
                .descripcion("Unmatched exclusivity. Up to 6 guests, private terrace, butler service, panoramic city view, private dining.")
                .descripcionEs("Exclusividad sin igual. Hasta 6 huéspedes, terraza privada, servicio de mayordomo, vista panorámica y comedor privado.")
                .imagenUrl("/images/hotel/suite-presidencial-4x3.jpg")
                .build());
    }

    // ===== 10 clientes =====
    private void guardarCliente(Cliente cliente, String rawPassword) {
        cliente.setUser(registrarUsuario(cliente.getEmail(), rawPassword, rolCliente));
        clienteRepository.save(cliente);
    }

    private void cargarClientes() {
        guardarCliente(Cliente.builder()
                .nombre("Carlos").apellido("Mendoza").cedula("1020304050").email("carlos.mendoza@email.com")
                .password("pass1234").telefono("+1-555-0101").fechaRegistro(LocalDate.of(2025, 1, 15))
                .build(), "pass1234");

        guardarCliente(Cliente.builder()
                .nombre("Ana").apellido("García").cedula("1020304051").email("ana.garcia@email.com")
                .password("pass1234").telefono("+1-555-0102").fechaRegistro(LocalDate.of(2025, 2, 20))
                .build(), "pass1234");

        guardarCliente(Cliente.builder()
                .nombre("Luis").apellido("Hernández").cedula("1020304052").email("luis.hernandez@email.com")
                .password("pass1234").telefono("+1-555-0103").fechaRegistro(LocalDate.of(2025, 3, 5))
                .build(), "pass1234");

        guardarCliente(Cliente.builder()
                .nombre("Sofia").apellido("Torres").cedula("1020304053").email("sofia.torres@email.com")
                .password("pass1234").telefono("+1-555-0104").fechaRegistro(LocalDate.of(2025, 3, 18))
                .build(), "pass1234");

        guardarCliente(Cliente.builder()
                .nombre("Miguel").apellido("Ramírez").cedula("1020304054").email("miguel.ramirez@email.com")
                .password("pass1234").telefono("+1-555-0105").fechaRegistro(LocalDate.of(2025, 4, 2))
                .build(), "pass1234");

        guardarCliente(Cliente.builder()
                .nombre("Isabella").apellido("López").cedula("1020304055").email("isabella.lopez@email.com")
                .password("pass1234").telefono("+1-555-0106").fechaRegistro(LocalDate.of(2025, 4, 25))
                .build(), "pass1234");

        guardarCliente(Cliente.builder()
                .nombre("Andrés").apellido("Martínez").cedula("1020304056").email("andres.martinez@email.com")
                .password("pass1234").telefono("+1-555-0107").fechaRegistro(LocalDate.of(2025, 5, 10))
                .build(), "pass1234");

        guardarCliente(Cliente.builder()
                .nombre("Valentina").apellido("Rodríguez").cedula("1020304057").email("valentina.rodriguez@email.com")
                .password("pass1234").telefono("+1-555-0108").fechaRegistro(LocalDate.of(2025, 6, 1))
                .build(), "pass1234");

        guardarCliente(Cliente.builder()
                .nombre("Jorge").apellido("Sánchez").cedula("1020304058").email("jorge.sanchez@email.com")
                .password("pass1234").telefono("+1-555-0109").fechaRegistro(LocalDate.of(2025, 7, 14))
                .build(), "pass1234");

        guardarCliente(Cliente.builder()
                .nombre("Camila").apellido("Díaz").cedula("1020304059").email("camila.diaz@email.com")
                .password("pass1234").telefono("+1-555-0110").fechaRegistro(LocalDate.of(2025, 8, 22))
                .build(), "pass1234");

        // ===== Huéspedes para probar el plan de visita (ver cargarReservasParaPlan) =====
        // Van al final para no mover los índices clientes.get(0..9) que usan las demás reservas.
        guardarCliente(Cliente.builder()
                .nombre("Daniel").apellido("Castro").cedula("1020304060").email("daniel.castro@email.com")
                .password("pass1234").telefono("+1-555-0111").fechaRegistro(LocalDate.of(2025, 9, 3))
                .build(), "pass1234");

        guardarCliente(Cliente.builder()
                .nombre("Lucía").apellido("Moreno").cedula("1020304061").email("lucia.moreno@email.com")
                .password("pass1234").telefono("+1-555-0112").fechaRegistro(LocalDate.of(2025, 9, 18))
                .build(), "pass1234");

        guardarCliente(Cliente.builder()
                .nombre("Mateo").apellido("Vargas").cedula("1020304062").email("mateo.vargas@email.com")
                .password("pass1234").telefono("+1-555-0113").fechaRegistro(LocalDate.of(2025, 10, 2))
                .build(), "pass1234");

        guardarCliente(Cliente.builder()
                .nombre("Elena").apellido("Ruiz").cedula("1020304063").email("elena.ruiz@email.com")
                .password("pass1234").telefono("+1-555-0114").fechaRegistro(LocalDate.of(2025, 10, 20))
                .build(), "pass1234");

        guardarCliente(Cliente.builder()
                .nombre("Tomás").apellido("Herrera").cedula("1020304064").email("tomas.herrera@email.com")
                .password("pass1234").telefono("+1-555-0115").fechaRegistro(LocalDate.of(2025, 11, 1))
                .build(), "pass1234");
    }

    // ===== 50 habitaciones (10 por cada uno de los 5 tipos) =====
    private void cargarHabitaciones() {
        // Recupera los tipos ya guardados, en el mismo orden en que se insertaron arriba
        TipoHabitacion normal = tipoHabitacionRepository.findAll().get(0);
        TipoHabitacion executive = tipoHabitacionRepository.findAll().get(1);
        TipoHabitacion vip = tipoHabitacionRepository.findAll().get(2);
        TipoHabitacion luxury = tipoHabitacionRepository.findAll().get(3);
        TipoHabitacion presidential = tipoHabitacionRepository.findAll().get(4);

        // ----- Normal: piso 1-2, $120-$140 -----
        guardarHabitacion("101", normal, 120.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("102", normal, 120.00, EstadoHabitacion.OCUPADA);
        guardarHabitacion("103", normal, 125.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("104", normal, 125.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("105", normal, 130.00, EstadoHabitacion.MANTENIMIENTO);
        guardarHabitacion("106", normal, 130.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("107", normal, 135.00, EstadoHabitacion.OCUPADA);
        guardarHabitacion("108", normal, 135.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("109", normal, 140.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("110", normal, 140.00, EstadoHabitacion.DISPONIBLE);

        // ----- Executive: piso 3-4, $190-$220 -----
        guardarHabitacion("301", executive, 190.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("302", executive, 190.00, EstadoHabitacion.OCUPADA);
        guardarHabitacion("303", executive, 195.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("304", executive, 195.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("305", executive, 200.00, EstadoHabitacion.MANTENIMIENTO);
        guardarHabitacion("306", executive, 200.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("307", executive, 205.00, EstadoHabitacion.OCUPADA);
        guardarHabitacion("308", executive, 210.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("309", executive, 215.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("310", executive, 220.00, EstadoHabitacion.DISPONIBLE);

        // ----- VIP: piso 5-6, $280-$320 -----
        guardarHabitacion("501", vip, 280.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("502", vip, 280.00, EstadoHabitacion.OCUPADA);
        guardarHabitacion("503", vip, 285.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("504", vip, 290.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("505", vip, 295.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("506", vip, 295.00, EstadoHabitacion.MANTENIMIENTO);
        guardarHabitacion("507", vip, 300.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("508", vip, 305.00, EstadoHabitacion.OCUPADA);
        guardarHabitacion("509", vip, 310.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("510", vip, 320.00, EstadoHabitacion.DISPONIBLE);

        // ----- Luxury: piso 7-8, $350-$420 -----
        guardarHabitacion("701", luxury, 350.00, EstadoHabitacion.OCUPADA);
        guardarHabitacion("702", luxury, 355.00, EstadoHabitacion.OCUPADA);
        guardarHabitacion("703", luxury, 360.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("704", luxury, 370.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("705", luxury, 380.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("706", luxury, 380.00, EstadoHabitacion.MANTENIMIENTO);
        guardarHabitacion("707", luxury, 390.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("708", luxury, 400.00, EstadoHabitacion.OCUPADA);
        guardarHabitacion("709", luxury, 410.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("710", luxury, 420.00, EstadoHabitacion.DISPONIBLE);

        // ----- Presidential Suite: piso 9-10, $800-$1200 -----
        guardarHabitacion("901", presidential, 800.00, EstadoHabitacion.OCUPADA);
        guardarHabitacion("902", presidential, 850.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("903", presidential, 900.00, EstadoHabitacion.OCUPADA);
        guardarHabitacion("904", presidential, 950.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("905", presidential, 1000.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("906", presidential, 1000.00, EstadoHabitacion.MANTENIMIENTO);
        guardarHabitacion("907", presidential, 1050.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("908", presidential, 1100.00, EstadoHabitacion.DISPONIBLE);
        guardarHabitacion("909", presidential, 1150.00, EstadoHabitacion.OCUPADA);
        guardarHabitacion("910", presidential, 1200.00, EstadoHabitacion.DISPONIBLE);
    }

    // ===== Método de ayuda para no repetir el .builder() 50 veces =====
    private void guardarHabitacion(String numero, TipoHabitacion tipo, double precio, EstadoHabitacion estado) {
        habitacionRepository.save(Habitacion.builder()
                .numero(numero)
                .tipoHabitacion(tipo)
                .precio(precio)
                .estado(estado)
                .build());
    }

    // ===== 5 servicios del hotel =====
    private void cargarServicios() {
        guardarServicio("Wellness",
                "Immerse yourself in total relaxation at The Royale Spa. Our full wellness sanctuary is designed around your wellbeing - from ancient thermal rituals to modern fitness, every detail is curated for the discerning guest.",
                45.0,
                "/images/hotel/spa-piscina-4x3.jpg",
                List.of(
                        "Heated Indoor Pool - Open daily 06:00 to 22:00",
                        "Finnish Sauna - Dry heat up to 90 degrees, private sessions available",
                        "Turkish Hammam - Traditional steam bath with aromatic oils",
                        "Aromatherapy Steam Room - Eucalyptus and lavender infusions",
                        "Full Gym 24h - Technogym equipment, personal trainer on request",
                        "Signature Massage - 60 or 90 min, Swedish, deep tissue and hot stone",
                        "Organic Facials - Premium skincare with ESPA and La Mer products",
                        "Private Couple Suite - Exclusive treatment room for two"
                ),
                List.of("/images/hotel/spa-piscina-16x9.jpg", "/images/hotel/spa-hammam-4x3.jpg", "/images/hotel/gimnasio-4x3.jpg"));

        guardarServicio("Dining",
                "Experience exceptional cuisine without leaving The Royale. Our signature restaurant serves contemporary New York cuisine from breakfast through late-night dining. In-room service is available 24 hours a day.",
                60.0,
                "/images/hotel/restaurante-4x3.jpg",
                List.of(
                        "24h Room Service - Full a la carte menu delivered to your suite",
                        "Signature Restaurant - Contemporary New York cuisine, breakfast to dinner",
                        "Private Dining Room - Exclusive setting for up to 12 guests",
                        "Premium Wine List - Over 200 labels from world-renowned vineyards",
                        "Craft Cocktail Bar - Handcrafted cocktails and spirits by our mixologists",
                        "In-Room Minibar - Curated selection refreshed daily",
                        "Private Bar Service - Butler-attended bar set up in your suite",
                        "Dietary Menus - Vegan, gluten-free and allergen-aware options available"
                ),
                List.of("/images/hotel/restaurante-16x9.jpg", "/images/hotel/restaurante-4x3.jpg", "/images/hotel/salon-ejecutivo-4x3.jpg"));

        guardarServicio("Business",
                "Stay productive from the heart of Manhattan. The Royale Business Center delivers everything corporate guests need - from state-of-the-art meeting technology to full executive support, all within steps of your room.",
                35.0,
                "/images/hotel/sala-juntas-4x3.jpg",
                List.of(
                        "Private Meeting Rooms - Up to 3 rooms, capacity 4 to 20 persons",
                        "4K Video Conferencing - Integrated Zoom and Teams, global connectivity",
                        "High-Speed Fiber Wi-Fi - Dedicated bandwidth up to 1 Gbps",
                        "Executive Lounge - Reserved workspace with panoramic Manhattan views",
                        "Printing and Secretarial - On-demand document handling and admin support",
                        "Business Center - 24h access, iMac workstations and ergonomic seating",
                        "Event Planning - Full AV setup and catering coordination",
                        "Concierge Business Support - Courier, notary and translation services"
                ),
                List.of("/images/hotel/sala-juntas-16x9.jpg", "/images/hotel/salon-ejecutivo-4x3.jpg", "/images/skyline.jpg"));

        guardarServicio("Concierge",
                "Our multilingual concierge team is available around the clock to orchestrate every aspect of your New York experience. No request is too extraordinary - we specialise in turning the impossible into the unforgettable.",
                25.0,
                "/images/hotel/recepcion-4x3.jpg",
                List.of(
                        "Broadway and Show Tickets - Priority access to sold-out performances",
                        "Fine Dining Reservations - Michelin-starred restaurants and exclusive tables",
                        "Private Museum Tours - After-hours access to MET, MoMA and more",
                        "Helicopter Rides - Scenic Manhattan flights from the East River helipad",
                        "Personal Shopping - Stylist-led experiences on Fifth Avenue",
                        "Airport Transfers - Chauffeured luxury vehicles, 24h availability",
                        "Floral and Gift Arrangements - Bespoke in-suite welcome experiences",
                        "Multilingual Assistance - Staff fluent in 8+ languages"
                ),
                List.of("/images/hotel/recepcion-16x9.jpg", "/images/hotel/galeria-4x3.jpg", "/images/broadway.jpg"));

        guardarServicio("Transportation",
                "Move through New York with the same ease and discretion you expect inside The Royale. Our transportation service coordinates private transfers, chauffeured vehicles and curated city routes for guests who value punctuality, comfort and privacy.",
                80.0,
                "/images/private-transport.jpg",
                List.of(
                        "Airport Transfers - Private arrivals and departures from JFK, LaGuardia and Newark",
                        "Chauffeured Vehicles - Luxury sedans and SUVs available by the hour",
                        "City Routes - Tailored itineraries across Manhattan, Brooklyn and beyond",
                        "Event Transfers - Coordinated pickups for galas, meetings and private dinners",
                        "Family Transport - Spacious vehicles with child seats on request",
                        "Executive Mobility - Quiet rides with Wi-Fi and bottled water",
                        "Late-Night Service - Reserved transport for evenings out in the city",
                        "Luggage Assistance - Door-to-door handling from room to vehicle"
                ),
                List.of("/images/private-transport.jpg", "/images/airport-transfer.jpg", "/images/private-transport.jpg"));
    }

    private void guardarServicio(String nombre, String descripcion, double precio, String imagenUrl,
            List<String> caracteristicas, List<String> galeriaUrls) {
        if (servicioRepository.existsByNombreIgnoreCase(nombre)) {
            return;
        }

        servicioRepository.save(Servicio.builder()
                .nombre(nombre)
                .descripcion(descripcion)
                // nombreEs se queda en null: los nombres de servicio no se traducen
                .descripcionEs(SERVICIOS_DESCRIPCION_ES.get(nombre))
                .precio(precio)
                .imagenUrl(imagenUrl)
                .caracteristicas(caracteristicas)
                .galeriaUrls(galeriaUrls)
                .build());
    }

    // ===== 5 operadores (personal del hotel; entran por /login y son redirigidos a /operator) =====
    private void guardarOperador(Operador operador, String rawPassword) {
        operador.setUser(registrarUsuario(operador.getEmail(), rawPassword, rolOperador));
        operadorRepository.save(operador);
    }

    private void cargarOperadores() {
        guardarOperador(Operador.builder()
                .nombre("Laura Gomez").email("laura.gomez@theroyale.com")
                .password("operator123").tipo(TipoOperador.OPERADOR)
                .build(), "operator123");

        guardarOperador(Operador.builder()
                .nombre("Pedro Ruiz").email("pedro.ruiz@theroyale.com")
                .password("operator123").tipo(TipoOperador.OPERADOR)
                .build(), "operator123");

        guardarOperador(Operador.builder()
                .nombre("Maria Castillo").email("maria.castillo@theroyale.com")
                .password("operator123").tipo(TipoOperador.OPERADOR)
                .build(), "operator123");

        guardarOperador(Operador.builder()
                .nombre("James Wright").email("james.wright@theroyale.com")
                .password("operator123").tipo(TipoOperador.OPERADOR)
                .build(), "operator123");

        guardarOperador(Operador.builder()
                .nombre("Sofia Chen").email("sofia.chen@theroyale.com")
                .password("operator123").tipo(TipoOperador.OPERADOR)
                .build(), "operator123");
    }

    // ===== Reservas de ejemplo en todos los estados, con fechas relativas al dia de arranque =====
    // Las habitaciones OCUPADA 102, 302 y 502 tienen una reserva EN_CURSO con su cuenta abierta.
    private void cargarReservas() {
        LocalDate hoy = LocalDate.now();
        Operador laura  = operadorRepository.findByEmail("laura.gomez@theroyale.com").orElseThrow();
        Operador pedro  = operadorRepository.findByEmail("pedro.ruiz@theroyale.com").orElseThrow();
        Operador maria  = operadorRepository.findByEmail("maria.castillo@theroyale.com").orElseThrow();
        Operador james  = operadorRepository.findByEmail("james.wright@theroyale.com").orElseThrow();
        Operador sofia  = operadorRepository.findByEmail("sofia.chen@theroyale.com").orElseThrow();
        List<Cliente> clientes = clienteRepository.findAll();
        Servicio wellness  = servicioRepository.findByNombreIgnoreCase("Wellness").orElseThrow();
        Servicio dining    = servicioRepository.findByNombreIgnoreCase("Dining").orElseThrow();
        Servicio concierge = servicioRepository.findByNombreIgnoreCase("Concierge").orElseThrow();
        Servicio business  = servicioRepository.findByNombreIgnoreCase("Business").orElseThrow();

        // ===== Llegan hoy =====
        // CONFIRMADA con servicios pre-reservados (cuenta abierta desde ayer)
        Reserva confirmada101 = guardarReserva(clientes.get(0), "101", hoy, hoy.plusDays(3), EstadoReserva.CONFIRMADA, 2);
        Cuenta cuenta101 = abrirCuenta(confirmada101, hoy.minusDays(1));
        agregarConsumo(cuenta101, concierge, laura, 1, false);

        // PENDIENTE (sin cuenta aun)
        guardarReserva(clientes.get(1), "301", hoy, hoy.plusDays(2), EstadoReserva.PENDIENTE, 1);

        // ===== Futuras =====
        guardarReserva(clientes.get(2), "103", hoy.plusDays(4), hoy.plusDays(7), EstadoReserva.PENDIENTE, 2);

        Reserva confirmada703 = guardarReserva(clientes.get(3), "703", hoy.plusDays(10), hoy.plusDays(14), EstadoReserva.CONFIRMADA, 3);
        Cuenta cuenta703 = abrirCuenta(confirmada703, hoy.minusDays(2));
        agregarConsumo(cuenta703, business, james, 2, false);

        Reserva confirmada901 = guardarReserva(clientes.get(9), "901", hoy.plusDays(5), hoy.plusDays(9), EstadoReserva.CONFIRMADA, 2);
        Cuenta cuenta901 = abrirCuenta(confirmada901, hoy.minusDays(1));
        agregarConsumo(cuenta901, wellness, sofia, 1, false);
        agregarConsumo(cuenta901, dining, sofia, 2, false);

        // ===== En curso =====
        // 102: sale hoy, pagada (lista para check-out)
        Reserva enCurso102 = guardarReserva(clientes.get(4), "102", hoy.minusDays(2), hoy, EstadoReserva.EN_CURSO, 2);
        Cuenta cuenta102 = abrirCuenta(enCurso102, hoy.minusDays(2));
        agregarConsumo(cuenta102, wellness, laura, 2, true);
        agregarPago(cuenta102, laura, BigDecimal.valueOf(90), "CARD");

        // 302: saldo pendiente
        Reserva enCurso302 = guardarReserva(clientes.get(5), "302", hoy.minusDays(1), hoy.plusDays(2), EstadoReserva.EN_CURSO, 2);
        Cuenta cuenta302 = abrirCuenta(enCurso302, hoy.minusDays(1));
        agregarConsumo(cuenta302, dining, pedro, 2, false);
        agregarConsumo(cuenta302, concierge, pedro, 1, false);
        agregarPago(cuenta302, pedro, BigDecimal.valueOf(50), "CASH");

        // 502: sin consumos aun
        Reserva enCurso502 = guardarReserva(clientes.get(6), "502", hoy.minusDays(3), hoy.plusDays(1), EstadoReserva.EN_CURSO, 3);
        Cuenta cuenta502 = abrirCuenta(enCurso502, hoy.minusDays(3));
        agregarConsumo(cuenta502, wellness, maria, 1, false);
        agregarPago(cuenta502, maria, BigDecimal.valueOf(45), "TRANSFER");

        // 701: en curso, gestionada por James
        Reserva enCurso701 = guardarReserva(clientes.get(7), "701", hoy.minusDays(1), hoy.plusDays(3), EstadoReserva.EN_CURSO, 2);
        Cuenta cuenta701 = abrirCuenta(enCurso701, hoy.minusDays(1));
        agregarConsumo(cuenta701, business, james, 1, false);
        agregarConsumo(cuenta701, dining, james, 1, false);
        agregarPago(cuenta701, james, BigDecimal.valueOf(80), "CARD");

        // ===== Historial =====
        Reserva finalizada104 = guardarReserva(clientes.get(7), "104", hoy.minusDays(9), hoy.minusDays(6), EstadoReserva.FINALIZADA, 2);
        Cuenta cuentaFinalizada104 = abrirCuenta(finalizada104, hoy.minusDays(9));
        agregarConsumo(cuentaFinalizada104, dining, laura, 1, true);
        agregarPago(cuentaFinalizada104, laura, BigDecimal.valueOf(60), "CARD");
        cuentaFinalizada104.setEstado(EstadoCuenta.CERRADA);
        cuentaRepository.save(cuentaFinalizada104);

        Reserva finalizada501 = guardarReserva(clientes.get(8), "501", hoy.minusDays(7), hoy.minusDays(4), EstadoReserva.FINALIZADA, 2);
        Cuenta cuentaFinalizada501 = abrirCuenta(finalizada501, hoy.minusDays(7));
        agregarConsumo(cuentaFinalizada501, concierge, sofia, 2, true);
        agregarPago(cuentaFinalizada501, sofia, BigDecimal.valueOf(50), "TRANSFER");
        cuentaFinalizada501.setEstado(EstadoCuenta.CERRADA);
        cuentaRepository.save(cuentaFinalizada501);

        // CANCELADA
        guardarReserva(clientes.get(8), "303", hoy.plusDays(1), hoy.plusDays(3), EstadoReserva.CANCELADA, 1);

        // ===== Más estadías terminadas, para que los tipos tengan reseñas (cuenta cerrada y sin saldo) =====
        Reserva finalizada106 = guardarEstadiaFinalizada(clientes.get(2), "106", hoy.minusDays(30), hoy.minusDays(27), 2);
        Reserva finalizada503 = guardarEstadiaFinalizada(clientes.get(3), "503", hoy.minusDays(25), hoy.minusDays(22), 3);
        Reserva finalizada303 = guardarEstadiaFinalizada(clientes.get(4), "303", hoy.minusDays(20), hoy.minusDays(18), 1);
        Reserva finalizada304 = guardarEstadiaFinalizada(clientes.get(6), "304", hoy.minusDays(35), hoy.minusDays(33), 2);
        Reserva finalizada704 = guardarEstadiaFinalizada(clientes.get(1), "704", hoy.minusDays(40), hoy.minusDays(36), 2);
        Reserva finalizada705 = guardarEstadiaFinalizada(clientes.get(9), "705", hoy.minusDays(15), hoy.minusDays(12), 2);

        // Estadías terminadas SIN reseña: el huésped puede reseñarlas desde "My reservations".
        // La de la Presidential Suite deja ese tipo sin reseñas ("No reviews yet").
        guardarEstadiaFinalizada(clientes.get(1), "108", hoy.minusDays(12), hoy.minusDays(10), 1);
        guardarEstadiaFinalizada(clientes.get(5), "902", hoy.minusDays(60), hoy.minusDays(55), 4);

        cargarResenas(finalizada104, finalizada501, finalizada106, finalizada503,
                finalizada303, finalizada304, finalizada704, finalizada705);
    }

    // ===== Reservas sin plan para probar "Plan my visit", una por cada caso del planificador =====
    // Ninguna tiene plan todavía (la 703 de Sofia ya trae el de ejemplo) y no tocan las habitaciones
    // ni las fechas que usan los E2E (Normal entre hoy+7 y hoy+11, la 101, la 902 y la 901).
    private void cargarReservasParaPlan() {
        LocalDate hoy = LocalDate.now();
        Cliente daniel = clienteRepository.findByEmailIgnoreCase("daniel.castro@email.com").orElseThrow();
        Cliente lucia = clienteRepository.findByEmailIgnoreCase("lucia.moreno@email.com").orElseThrow();
        Cliente mateo = clienteRepository.findByEmailIgnoreCase("mateo.vargas@email.com").orElseThrow();
        Cliente elena = clienteRepository.findByEmailIgnoreCase("elena.ruiz@email.com").orElseThrow();
        Cliente tomas = clienteRepository.findByEmailIgnoreCase("tomas.herrera@email.com").orElseThrow();

        // Llega en 3 días: clima con el pronóstico de NWS. Cuenta abierta, así que "Add to my stay" funciona
        Reserva pronosticoNws = guardarReserva(daniel, "707", hoy.plusDays(3), hoy.plusDays(6), EstadoReserva.CONFIRMADA, 2);
        abrirCuenta(pronosticoNws, hoy.minusDays(1));

        // Llega en 12 días: clima con el pronóstico de Open-Meteo (días 8 a 16)
        Reserva pronosticoOpenMeteo = guardarReserva(lucia, "504", hoy.plusDays(12), hoy.plusDays(16), EstadoReserva.CONFIRMADA, 3);
        abrirCuenta(pronosticoOpenMeteo, hoy.minusDays(1));

        // Llega en 45 días: muy lejos para un pronóstico, se usa el clima típico de esas fechas
        Reserva climaTipico = guardarReserva(mateo, "306", hoy.plusDays(45), hoy.plusDays(48), EstadoReserva.CONFIRMADA, 2);
        abrirCuenta(climaTipico, hoy.minusDays(1));

        // PENDIENTE: el plan se puede armar, pero "Add to my stay" queda deshabilitado (sin cuenta abierta)
        guardarReserva(elena, "109", hoy.plusDays(20), hoy.plusDays(23), EstadoReserva.PENDIENTE, 2);

        // EN_CURSO desde ayer: solo se planifican los días que faltan. La suite queda OCUPADA, como tras el check-in
        Reserva enCurso = guardarReserva(tomas, "904", hoy.minusDays(1), hoy.plusDays(3), EstadoReserva.EN_CURSO, 4);
        abrirCuenta(enCurso, hoy.minusDays(1));
        Habitacion suite904 = enCurso.getHabitacion();
        suite904.setEstado(EstadoHabitacion.OCUPADA);
        habitacionRepository.save(suite904);
    }

    // ===== Reseñas de ejemplo: Normal, Executive, VIP y Luxury tienen promedio; Presidential no =====
    // La de la 705 está oculta por el admin: no se ve en /rooms ni cuenta para el promedio de Luxury.
    private void cargarResenas(Reserva finalizada104, Reserva finalizada501, Reserva finalizada106,
                               Reserva finalizada503, Reserva finalizada303, Reserva finalizada304,
                               Reserva finalizada704, Reserva finalizada705) {
        guardarResena(finalizada104, 4, "Great value in Manhattan",
                "Comfortable bed, quiet room and a very helpful front desk. The bathroom could be a bit bigger.", true);
        guardarResena(finalizada501, 5, "Perfect city view",
                "Spotless room and very attentive staff. The living area was ideal for working in the evenings.", true);
        guardarResena(finalizada106, 5, "Better than expected",
                "Elegant and cozy room, excellent Wi-Fi and the breakfast in the restaurant was delicious.", true);
        guardarResena(finalizada503, 4, "Lovely stay with the family",
                "Plenty of space for three people and great amenities. Check-in took a little longer than expected.", true);
        guardarResena(finalizada303, 4, "Ideal for a business trip",
                "The workspace is well designed and the room is very quiet. I would stay here again for work.", true);
        guardarResena(finalizada304, 3, null,
                "Good location and a nice desk, but the air conditioning was noisy during the night.", true);
        guardarResena(finalizada704, 5, "Pure luxury",
                "The jacuzzi and the view over the city were unforgettable. Impeccable service from start to finish.", true);
        guardarResena(finalizada705, 1, "Not happy",
                "This review contains language that the hotel considered inappropriate and was hidden by the admin.", false);
    }

    private void guardarResena(Reserva reserva, int calificacion, String titulo, String comentario, boolean visible) {
        resenaRepository.save(Resena.builder()
                .reserva(reserva)
                .cliente(reserva.getCliente())
                .tipoHabitacion(reserva.getHabitacion().getTipoHabitacion())
                .calificacion(calificacion)
                .titulo(titulo)
                .comentario(comentario)
                .visible(visible)
                .fechaCreacion(reserva.getFechaFin().atTime(18, 30))
                .build());
    }

    // Reserva FINALIZADA con su cuenta ya cerrada, como queda tras el check-out
    private Reserva guardarEstadiaFinalizada(Cliente cliente, String numeroHabitacion,
                                             LocalDate inicio, LocalDate fin, int personas) {
        Reserva reserva = guardarReserva(cliente, numeroHabitacion, inicio, fin, EstadoReserva.FINALIZADA, personas);
        Cuenta cuenta = abrirCuenta(reserva, inicio);
        cuenta.setEstado(EstadoCuenta.CERRADA);
        cuentaRepository.save(cuenta);
        return reserva;
    }

    private Reserva guardarReserva(Cliente cliente, String numeroHabitacion,
                                   LocalDate inicio, LocalDate fin, EstadoReserva estado, int personas) {
        Habitacion habitacion = habitacionRepository.findByNumero(numeroHabitacion).orElseThrow();
        return reservaRepository.save(Reserva.builder()
                .cliente(cliente)
                .habitacion(habitacion)
                .fechaInicio(inicio)
                .fechaFin(fin)
                .estado(estado)
                .cantidadPersonas(personas)
                .precioNocheAcordado(BigDecimal.valueOf(habitacion.getPrecio()))
                .fechaCreacion(LocalDateTime.now().minusDays(15))
                .build());
    }

    private Cuenta abrirCuenta(Reserva reserva, LocalDate desde) {
        return cuentaRepository.save(Cuenta.builder()
                .reserva(reserva)
                .estado(EstadoCuenta.ABIERTA)
                .fechaCreacion(desde.atTime(15, 0))
                .itemsConsumo(new ArrayList<>())
                .pagos(new ArrayList<>())
                .build());
    }

    private void agregarConsumo(Cuenta cuenta, Servicio servicio, Operador operador, int cantidad, boolean pagado) {
        cuenta.getItemsConsumo().add(ItemConsumo.builder()
                .cuenta(cuenta)
                .servicio(servicio)
                .operador(operador)
                .cantidad(cantidad)
                .fechaHora(LocalDateTime.now().minusHours(5))
                .precioUnitario(BigDecimal.valueOf(servicio.getPrecio()))
                .pagado(pagado)
                .build());
        cuentaRepository.save(cuenta);
    }

    private void agregarPago(Cuenta cuenta, Operador operador, BigDecimal monto, String metodo) {
        cuenta.getPagos().add(Pago.builder()
                .cuenta(cuenta)
                .operador(operador)
                .monto(monto)
                .fecha(LocalDateTime.now().minusHours(2))
                .metodoPago(metodo)
                .build());
        cuentaRepository.save(cuenta);
    }

    // =========================================================================================
    // ===== Historial de 90 días para que el dashboard de dirección tenga tendencias =====
    //
    // El resto del DataLoader siembra el presente (unas 24 reservas alrededor de hoy), que basta
    // para probar las pantallas pero deja el panel de dirección sin nada que graficar: sin pasado
    // no hay tendencia, ni período anterior con el que comparar, ni ocupación media creíble.
    //
    // Reglas que se respetan a propósito, porque hay pruebas que dependen de los datos existentes:
    //
    //  1. Solo fechas PASADAS, en [hoy-90, hoy-1]. No se crea ni una reserva que toque hoy o el
    //     futuro, así no interfiere con la ventana de disponibilidad (tipo Normal entre hoy+7 y
    //     hoy+11) que busca NuevoUsuarioReservaE2ETest.
    //  2. Nunca se usan las habitaciones 101, 901 y 902: son las que los E2E tienen escritas a mano
    //     (EstadiaOperadorE2ETest hace el check-in de la 101, PlanVisitaE2ETest usa la 901).
    //  3. No se inserta ningún cliente: solo se leen los que ya existen, para no mover los índices
    //     clientes.get(0..9) de los que dependen las demás reservas y los E2E.
    //  4. Este método se llama al FINAL de run(), cuando todo lo demás ya está sembrado.
    //
    // El Random lleva semilla fija: dos arranques dan exactamente los mismos datos, así una cifra
    // del panel se puede verificar a mano y sigue valiendo mañana.
    //
    // Estacionalidad: viernes y sábados se llenan mucho más que el resto de la semana, como en un
    // hotel de verdad, para que la gráfica de ocupación tenga forma y no sea una línea plana.
    // =========================================================================================

    private static final long SEMILLA_HISTORIAL = 20261123L;
    private static final int DIAS_HISTORIAL = 90;

    // Las que los E2E Selenium tienen escritas a mano: el historial no las toca
    private static final List<String> HABITACIONES_RESERVADAS_E2E = List.of("101", "901", "902");

    // Ventana futura que la gráfica de ocupación muestra como ya reservado
    private static final int DIAS_ON_THE_BOOKS = 30;

    // NuevoUsuarioReservaE2ETest busca disponibilidad de este tipo: el seed no lo ocupa nunca
    private static final String TIPO_PROTEGIDO_E2E = "Normal";

    // ResenaHuespedE2ETest exige que este tipo no tenga NINGUNA reseña visible: su primer paso
    // comprueba que la página pública muestre "No reviews yet" antes de que el huésped escriba la
    // suya. El historial sí le crea reservas e ingresos (alimentan el desglose), pero nunca reseñas.
    private static final String TIPO_SIN_RESENAS_E2E = "Presidential Suite";

    private static final List<String> METODOS_PAGO = List.of("CARD", "CASH", "TRANSFER");

    private static final List<String> RESENAS_BUENAS = List.of(
            "Flawless from start to finish|The staff remembered our names and the room was spotless every single day.",
            "Worth every penny|Quiet, elegant and perfectly located. We will be coming back in the spring.",
            "A proper boutique hotel|Breakfast was excellent and the concierge found us tickets we could not get ourselves.",
            "Exceeded expectations|The bed, the linens, the silence. We slept better here than at home.");

    private static final List<String> RESENAS_REGULARES = List.of(
            "Lovely room, slow service|The room was beautiful but room service took almost an hour.",
            "Good stay with small details to fix|Great location and comfortable bed, though the bathroom light flickered.",
            "Nice but noisy|Everything was pleasant except the street noise in the early morning.");

    private static final List<String> RESENAS_MALAS = List.of(
            "Room was not ready|We waited two hours past check-in and nobody explained why.",
            "Not what was promised|The suite we booked was unavailable and the replacement was much smaller.");


    private void cargarHistorialDashboard() {
        Random aleatorio = new Random(SEMILLA_HISTORIAL);
        LocalDate hoy = LocalDate.now();

        List<Cliente> clientes = clienteRepository.findAll();
        List<Operador> operadores = operadorRepository.findAll();
        List<Servicio> servicios = servicioRepository.findAll();

        // Si alguien cambia el orden de run() y este método queda primero, se sale sin hacer nada
        // en lugar de reventar el arranque de la aplicación.
        if (clientes.isEmpty() || operadores.isEmpty() || servicios.isEmpty()) {
            return;
        }

        List<Habitacion> habitaciones = habitacionRepository.findAllByOrderByNumeroAsc().stream()
                .filter(habitacion -> habitacion.getEstado() != EstadoHabitacion.DESHABILITADA)
                .filter(habitacion -> !HABITACIONES_RESERVADAS_E2E.contains(habitacion.getNumero()))
                .toList();

        int reservasCreadas = 0;
        int resenasNegativasRecientes = 0;

        for (Habitacion habitacion : habitaciones) {
            LocalDate cursor = hoy.minusDays(DIAS_HISTORIAL);

            // Se avanza habitación por habitación colocando estadías seguidas con huecos entre
            // ellas: así, por construcción, una habitación nunca queda con dos reservas solapadas.
            while (cursor.isBefore(hoy)) {
                boolean finDeSemana = esFinDeSemana(cursor);
                if (aleatorio.nextDouble() > (finDeSemana ? 0.92 : 0.55)) {
                    cursor = cursor.plusDays(1);
                    continue;
                }

                int noches = 2 + aleatorio.nextInt(5);           // de 2 a 6 noches
                LocalDate fin = cursor.plusDays(noches);
                if (!fin.isBefore(hoy)) {
                    break;                                       // nada que toque hoy ni el futuro
                }

                Cliente cliente = clientes.get(aleatorio.nextInt(clientes.size()));
                int personas = 1 + aleatorio.nextInt(3);

                // Lead time variado para que el KPI no salga constante
                LocalDateTime creada = cursor.minusDays(2L + aleatorio.nextInt(44)).atTime(10, 30);

                // Una de cada diez se cancela: suficiente para que la tasa de cancelación sea real
                if (aleatorio.nextInt(10) == 0) {
                    guardarReservaHistorica(cliente, habitacion, cursor, fin,
                            EstadoReserva.CANCELADA, personas, creada);
                    reservasCreadas++;
                    cursor = fin.plusDays(1L + aleatorio.nextInt(5));
                    continue;
                }

                Reserva reserva = guardarReservaHistorica(cliente, habitacion, cursor, fin,
                        EstadoReserva.FINALIZADA, personas, creada);
                reservasCreadas++;

                Cuenta cuenta = abrirCuenta(reserva, cursor);
                BigDecimal totalConsumido = BigDecimal.ZERO;
                int cuantosConsumos = aleatorio.nextInt(4);       // de 0 a 3 servicios por estadía
                for (int i = 0; i < cuantosConsumos; i++) {
                    Servicio servicio = servicios.get(aleatorio.nextInt(servicios.size()));
                    Operador operador = operadores.get(aleatorio.nextInt(operadores.size()));
                    LocalDateTime cuando = cursor.plusDays(aleatorio.nextInt(noches))
                            .atTime(9 + aleatorio.nextInt(12), 15);
                    totalConsumido = totalConsumido.add(agregarConsumoHistorico(
                            cuenta, servicio, operador, 1 + aleatorio.nextInt(3), cuando));
                }

                // Una reserva FINALIZADA solo puede quedar así con la cuenta saldada (saldo 0):
                // se cobra el total el día de la salida y la cuenta se cierra.
                if (totalConsumido.signum() > 0) {
                    agregarPagoHistorico(cuenta,
                            operadores.get(aleatorio.nextInt(operadores.size())),
                            totalConsumido,
                            METODOS_PAGO.get(aleatorio.nextInt(METODOS_PAGO.size())),
                            fin.atTime(11, 0));
                }
                cuenta.setEstado(EstadoCuenta.CERRADA);
                cuentaRepository.save(cuenta);

                // Cuatro de cada diez huéspedes dejan reseña, salvo en el tipo que los E2E
                // necesitan ver sin ninguna
                boolean tipoAdmiteResenas = !TIPO_SIN_RESENAS_E2E.equals(
                        habitacion.getTipoHabitacion().getNombre());
                if (tipoAdmiteResenas && aleatorio.nextInt(10) < 4) {
                    // Dos quejas recientes a propósito: sin ellas la alerta de reseñas de 1 y 2
                    // estrellas nunca se vería en el panel, y es una de las más útiles.
                    boolean recienteYNegativa = fin.isAfter(hoy.minusDays(7))
                            && resenasNegativasRecientes < RESENAS_MALAS.size();
                    if (recienteYNegativa) {
                        guardarResenaHistorica(reserva, 1 + aleatorio.nextInt(2),
                                RESENAS_MALAS.get(resenasNegativasRecientes));
                        resenasNegativasRecientes++;
                    } else if (aleatorio.nextInt(5) == 0) {
                        guardarResenaHistorica(reserva, 3,
                                RESENAS_REGULARES.get(aleatorio.nextInt(RESENAS_REGULARES.size())));
                    } else {
                        guardarResenaHistorica(reserva, 4 + aleatorio.nextInt(2),
                                RESENAS_BUENAS.get(aleatorio.nextInt(RESENAS_BUENAS.size())));
                    }
                }

                cursor = fin.plusDays(1L + aleatorio.nextInt(5));
            }
        }

        int futuras = cargarReservasFuturasParaOnTheBooks(aleatorio, hoy, clientes);

        System.out.println("DataLoader: historial del dashboard cargado ("
                + reservasCreadas + " reservas en los ultimos " + DIAS_HISTORIAL + " dias, "
                + futuras + " futuras para el on the books)");
    }

    // Viernes y sábado son los días que llenan un hotel de ciudad
    private boolean esFinDeSemana(LocalDate fecha) {
        return fecha.getDayOfWeek() == DayOfWeek.FRIDAY || fecha.getDayOfWeek() == DayOfWeek.SATURDAY;
    }

    // ===== Variantes con fecha explícita de los helpers de arriba =====
    // Los originales fijan la fecha a LocalDateTime.now() menos unas horas, que es lo que necesitan
    // las reservas del presente. El historial necesita colocar cada movimiento en su día real, o
    // todas las series del dashboard se apilarían en la fecha de hoy.

    private Reserva guardarReservaHistorica(Cliente cliente, Habitacion habitacion,
                                            LocalDate inicio, LocalDate fin,
                                            EstadoReserva estado, int personas, LocalDateTime creada) {
        return reservaRepository.save(Reserva.builder()
                .cliente(cliente)
                .habitacion(habitacion)
                .fechaInicio(inicio)
                .fechaFin(fin)
                .estado(estado)
                .cantidadPersonas(personas)
                .precioNocheAcordado(BigDecimal.valueOf(habitacion.getPrecio()))
                .fechaCreacion(creada)
                .build());
    }

    // Devuelve el importe del consumo, para poder saldar la cuenta exactamente
    private BigDecimal agregarConsumoHistorico(Cuenta cuenta, Servicio servicio, Operador operador,
                                               int cantidad, LocalDateTime fechaHora) {
        BigDecimal precioUnitario = BigDecimal.valueOf(servicio.getPrecio());
        cuenta.getItemsConsumo().add(ItemConsumo.builder()
                .cuenta(cuenta)
                .servicio(servicio)
                .operador(operador)
                .cantidad(cantidad)
                .fechaHora(fechaHora)
                .precioUnitario(precioUnitario)
                .pagado(true)
                .build());
        cuentaRepository.save(cuenta);
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    private void agregarPagoHistorico(Cuenta cuenta, Operador operador, BigDecimal monto,
                                      String metodo, LocalDateTime fecha) {
        cuenta.getPagos().add(Pago.builder()
                .cuenta(cuenta)
                .operador(operador)
                .monto(monto)
                .fecha(fecha)
                .metodoPago(metodo)
                .build());
        cuentaRepository.save(cuenta);
    }

    // El texto viene como "titulo|comentario" para tenerlos juntos en la lista de ejemplos
    private void guardarResenaHistorica(Reserva reserva, int calificacion, String tituloYComentario) {
        String[] partes = tituloYComentario.split("\\|", 2);
        resenaRepository.save(Resena.builder()
                .reserva(reserva)
                .cliente(reserva.getCliente())
                .tipoHabitacion(reserva.getHabitacion().getTipoHabitacion())
                .calificacion(calificacion)
                .titulo(partes[0])
                .comentario(partes[1])
                .visible(true)
                .fechaCreacion(reserva.getFechaFin().atTime(18, 30))
                .build());
    }

    // =========================================================================================
    // ===== Reservas futuras: lo que la gráfica de ocupación muestra como "on the books" =====
    //
    // El historial de arriba es solo pasado, así que sin esto la zona futura de la gráfica saldría
    // plana en cero y el administrador no vería lo que ya está vendido, que es justo lo que le
    // sirve para decidir tarifas esta semana.
    //
    // Tres candados para no romper los E2E Selenium, que buscan disponibilidad real:
    //
    //  1. Solo tipos distintos de "Normal". NuevoUsuarioReservaE2ETest busca una habitación Normal
    //     entre hoy+7 y hoy+11: si este método ocupara alguna, esa prueba fallaría.
    //  2. Nunca las habitaciones 101, 901 ni 902 (las que los E2E tienen escritas a mano).
    //  3. Se salta cualquier habitación que ya tenga una reserva futura, sea del DataLoader o de
    //     los E2E, así que tampoco puede solaparse con lo ya sembrado.
    //
    // La densidad baja con la distancia: los próximos días están casi llenos y a 30 días vista
    // queda poco vendido. Es la curva real de reservas de un hotel, y hace que la zona sombreada
    // de la gráfica se lea como una rampa descendente en lugar de un bloque uniforme.
    // =========================================================================================
    private int cargarReservasFuturasParaOnTheBooks(Random aleatorio, LocalDate hoy,
                                                    List<Cliente> clientes) {
        // Cualquier habitación con algo ya reservado de hoy en adelante queda intocable
        Set<Long> yaOcupadasEnElFuturo = reservaRepository.findAll().stream()
                .filter(reserva -> reserva.getEstado() != EstadoReserva.CANCELADA)
                .filter(reserva -> !reserva.getFechaFin().isBefore(hoy))
                .map(reserva -> reserva.getHabitacion().getId())
                .collect(Collectors.toSet());

        List<Habitacion> habitaciones = habitacionRepository.findAllByOrderByNumeroAsc().stream()
                .filter(habitacion -> habitacion.getEstado() != EstadoHabitacion.DESHABILITADA)
                .filter(habitacion -> !HABITACIONES_RESERVADAS_E2E.contains(habitacion.getNumero()))
                .filter(habitacion -> !TIPO_PROTEGIDO_E2E.equals(habitacion.getTipoHabitacion().getNombre()))
                .filter(habitacion -> !yaOcupadasEnElFuturo.contains(habitacion.getId()))
                .toList();

        int creadas = 0;
        for (Habitacion habitacion : habitaciones) {
            LocalDate cursor = hoy.plusDays(1);
            while (cursor.isBefore(hoy.plusDays(DIAS_ON_THE_BOOKS))) {
                long diasAdelante = ChronoUnit.DAYS.between(hoy, cursor);

                // Curva de reservas: muy probable a pocos días, poco probable a un mes
                double probabilidad = (esFinDeSemana(cursor) ? 0.9 : 0.6)
                        * (1.0 - (diasAdelante / (double) (DIAS_ON_THE_BOOKS + 10)));
                if (aleatorio.nextDouble() > probabilidad) {
                    cursor = cursor.plusDays(1);
                    continue;
                }

                int noches = 2 + aleatorio.nextInt(4);
                LocalDate fin = cursor.plusDays(noches);
                if (!fin.isBefore(hoy.plusDays(DIAS_ON_THE_BOOKS + 1L))) {
                    break;
                }

                // La mayoría ya confirmadas; algunas quedan PENDIENTE, que es lo normal en una
                // reserva reciente y además da contenido a la alerta de "sin confirmar".
                EstadoReserva estado = aleatorio.nextInt(8) == 0
                        ? EstadoReserva.PENDIENTE
                        : EstadoReserva.CONFIRMADA;

                guardarReservaHistorica(
                        clientes.get(aleatorio.nextInt(clientes.size())),
                        habitacion, cursor, fin, estado,
                        1 + aleatorio.nextInt(3),
                        hoy.minusDays(aleatorio.nextInt(20)).atTime(16, 45));
                creadas++;

                cursor = fin.plusDays(1L + aleatorio.nextInt(4));
            }
        }
        return creadas;
    }
}
