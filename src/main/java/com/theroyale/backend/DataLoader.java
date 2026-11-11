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
import com.theroyale.backend.repository.AdminRepository;
import com.theroyale.backend.repository.ClienteRepository;
import com.theroyale.backend.repository.CuentaRepository;
import com.theroyale.backend.repository.HabitacionRepository;
import com.theroyale.backend.repository.LugarNycRepository;
import com.theroyale.backend.repository.OperadorRepository;
import com.theroyale.backend.repository.PlatoMenuRepository;
import com.theroyale.backend.repository.ResenaRepository;
import com.theroyale.backend.repository.ReservaRepository;
import com.theroyale.backend.repository.ServicioRepository;
import com.theroyale.backend.repository.TipoHabitacionRepository;
import com.theroyale.backend.service.plan.PlanGenerado;
import com.theroyale.backend.service.plan.PlanGenerado.BloqueGenerado;
import com.theroyale.backend.service.plan.PlanGenerado.DiaGenerado;
import com.theroyale.backend.service.plan.PlanVisitaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

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

    @Override
    public void run(String... args) throws Exception {
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
                "17.00", RestauranteHotel.SIGNATURE_RESTAURANT, CategoriaPlato.ENTRADA, true,
                EtiquetaPlato.VEGANO, EtiquetaPlato.SIN_GLUTEN);
        guardarPlato("Burrata and Heirloom Tomatoes", "Creamy burrata, heirloom tomatoes from the Hudson Valley, basil oil and sea salt.",
                "19.00", RestauranteHotel.SIGNATURE_RESTAURANT, CategoriaPlato.ENTRADA, true,
                EtiquetaPlato.VEGETARIANO, EtiquetaPlato.SIN_GLUTEN);
        guardarPlato("Yellowfin Tuna Tartare", "Hand-cut yellowfin tuna, avocado, yuzu dressing and sesame tuile.",
                "24.00", RestauranteHotel.SIGNATURE_RESTAURANT, CategoriaPlato.ENTRADA, true);
        guardarPlato("Dry-Aged New York Strip", "28-day dry-aged strip steak, roasted bone marrow butter and pommes puree.",
                "58.00", RestauranteHotel.SIGNATURE_RESTAURANT, CategoriaPlato.PRINCIPAL, true,
                EtiquetaPlato.SIN_GLUTEN);
        guardarPlato("Pan-Seared Atlantic Salmon", "Crispy-skin salmon, lemon beurre blanc, charred asparagus and fingerling potatoes.",
                "42.00", RestauranteHotel.SIGNATURE_RESTAURANT, CategoriaPlato.PRINCIPAL, true,
                EtiquetaPlato.SIN_GLUTEN);
        guardarPlato("Wild Mushroom Risotto", "Carnaroli rice, forest mushrooms, aged parmesan and white truffle oil.",
                "34.00", RestauranteHotel.SIGNATURE_RESTAURANT, CategoriaPlato.PRINCIPAL, true,
                EtiquetaPlato.VEGETARIANO, EtiquetaPlato.SIN_GLUTEN);
        guardarPlato("Spiced Cauliflower Steak", "Harissa-roasted cauliflower, tahini, pomegranate and herb quinoa.",
                "29.00", RestauranteHotel.SIGNATURE_RESTAURANT, CategoriaPlato.PRINCIPAL, true,
                EtiquetaPlato.VEGANO, EtiquetaPlato.SIN_GLUTEN, EtiquetaPlato.PICANTE);
        guardarPlato("Maine Lobster Linguine", "Fresh linguine, butter-poached Maine lobster, cherry tomatoes and chili. Seasonal.",
                "49.00", RestauranteHotel.SIGNATURE_RESTAURANT, CategoriaPlato.PRINCIPAL, false,
                EtiquetaPlato.PICANTE);
        guardarPlato("Classic New York Cheesecake", "Vanilla bean cheesecake on a graham cracker crust with berry compote.",
                "14.00", RestauranteHotel.SIGNATURE_RESTAURANT, CategoriaPlato.POSTRE, true,
                EtiquetaPlato.VEGETARIANO);
        guardarPlato("Dark Chocolate Mousse", "Silky 70% dark chocolate and coconut mousse with raspberries.",
                "13.00", RestauranteHotel.SIGNATURE_RESTAURANT, CategoriaPlato.POSTRE, true,
                EtiquetaPlato.VEGANO, EtiquetaPlato.SIN_GLUTEN);

        // Craft Cocktail Bar: cócteles de autor y algo para picar
        guardarPlato("The Royale Manhattan", "Rye whiskey, sweet vermouth, house bitters and a brandied cherry, stirred to order.",
                "22.00", RestauranteHotel.COCKTAIL_BAR, CategoriaPlato.BEBIDA, true,
                EtiquetaPlato.VEGANO, EtiquetaPlato.SIN_GLUTEN);
        guardarPlato("Fifth Avenue Spritz", "Elderflower liqueur, prosecco, white peach and soda.",
                "19.00", RestauranteHotel.COCKTAIL_BAR, CategoriaPlato.BEBIDA, true,
                EtiquetaPlato.VEGANO, EtiquetaPlato.SIN_GLUTEN);
        guardarPlato("Smoky Jalapeno Margarita", "Mezcal, fresh lime, agave and jalapeno with a chili-salt rim.",
                "20.00", RestauranteHotel.COCKTAIL_BAR, CategoriaPlato.BEBIDA, true,
                EtiquetaPlato.VEGANO, EtiquetaPlato.SIN_GLUTEN, EtiquetaPlato.PICANTE);
        guardarPlato("Garden Mocktail", "Alcohol-free blend of cucumber, mint, lime and sparkling tonic.",
                "12.00", RestauranteHotel.COCKTAIL_BAR, CategoriaPlato.BEBIDA, true,
                EtiquetaPlato.VEGANO, EtiquetaPlato.SIN_GLUTEN);
        guardarPlato("Truffle Parmesan Fries", "Hand-cut fries, black truffle, parmesan and rosemary aioli.",
                "14.00", RestauranteHotel.COCKTAIL_BAR, CategoriaPlato.ENTRADA, true,
                EtiquetaPlato.VEGETARIANO);
        guardarPlato("Wagyu Sliders", "Three mini wagyu burgers with aged cheddar and caramelized onions.",
                "24.00", RestauranteHotel.COCKTAIL_BAR, CategoriaPlato.ENTRADA, true);

        // 24h Room Service: carta a la habitación a cualquier hora
        guardarPlato("The Royale Club Sandwich", "Roasted turkey, bacon, avocado, tomato and herb mayo on toasted brioche, with fries.",
                "26.00", RestauranteHotel.ROOM_SERVICE, CategoriaPlato.PRINCIPAL, true);
        guardarPlato("Classic Cheeseburger", "Prime beef patty, cheddar, lettuce, tomato and house sauce, with fries.",
                "28.00", RestauranteHotel.ROOM_SERVICE, CategoriaPlato.PRINCIPAL, true);
        guardarPlato("Buddha Bowl", "Quinoa, roasted sweet potato, chickpeas, kale, avocado and miso-ginger dressing.",
                "24.00", RestauranteHotel.ROOM_SERVICE, CategoriaPlato.PRINCIPAL, true,
                EtiquetaPlato.VEGANO, EtiquetaPlato.SIN_GLUTEN);
        guardarPlato("Chicken Caesar Salad", "Grilled chicken, romaine hearts, parmesan, croutons and classic Caesar dressing.",
                "22.00", RestauranteHotel.ROOM_SERVICE, CategoriaPlato.PRINCIPAL, true);
        guardarPlato("Seasonal Fruit Platter", "A selection of fresh seasonal fruit with mint.",
                "15.00", RestauranteHotel.ROOM_SERVICE, CategoriaPlato.POSTRE, true,
                EtiquetaPlato.VEGANO, EtiquetaPlato.SIN_GLUTEN);

        // Breakfast Lounge
        guardarPlato("Eggs Benedict", "Poached eggs, smoked ham and hollandaise on a toasted English muffin.",
                "24.00", RestauranteHotel.BREAKFAST_LOUNGE, CategoriaPlato.DESAYUNO, true);
        guardarPlato("Avocado Toast", "Smashed avocado, cherry tomatoes, chili flakes and microgreens on sourdough.",
                "19.00", RestauranteHotel.BREAKFAST_LOUNGE, CategoriaPlato.DESAYUNO, true,
                EtiquetaPlato.VEGANO);
        guardarPlato("Buttermilk Pancakes", "Fluffy stack with Vermont maple syrup, berries and whipped butter.",
                "18.00", RestauranteHotel.BREAKFAST_LOUNGE, CategoriaPlato.DESAYUNO, true,
                EtiquetaPlato.VEGETARIANO);
        guardarPlato("Acai Bowl", "Acai, banana, toasted seed crunch, fresh berries and coconut.",
                "16.00", RestauranteHotel.BREAKFAST_LOUNGE, CategoriaPlato.DESAYUNO, true,
                EtiquetaPlato.VEGANO, EtiquetaPlato.SIN_GLUTEN);
        guardarPlato("Smoked Salmon Bagel", "New York bagel, smoked salmon, cream cheese, capers and red onion.",
                "22.00", RestauranteHotel.BREAKFAST_LOUNGE, CategoriaPlato.DESAYUNO, true);
        guardarPlato("Greek Yogurt Parfait", "Greek yogurt, gluten-free granola, honey and seasonal berries.",
                "14.00", RestauranteHotel.BREAKFAST_LOUNGE, CategoriaPlato.DESAYUNO, true,
                EtiquetaPlato.VEGETARIANO, EtiquetaPlato.SIN_GLUTEN);
        guardarPlato("Cold-Pressed Green Juice", "Kale, cucumber, green apple, celery, lemon and ginger.",
                "11.00", RestauranteHotel.BREAKFAST_LOUNGE, CategoriaPlato.BEBIDA, true,
                EtiquetaPlato.VEGANO, EtiquetaPlato.SIN_GLUTEN);
    }

    private void guardarPlato(String nombre, String descripcion, String precio, RestauranteHotel restaurante,
            CategoriaPlato categoria, boolean disponible, EtiquetaPlato... etiquetas) {
        platoMenuRepository.save(PlatoMenu.builder()
                .nombre(nombre)
                .descripcion(descripcion)
                .precio(new BigDecimal(precio))
                .restaurante(restaurante)
                .categoria(categoria)
                .disponible(disponible)
                .etiquetas(new LinkedHashSet<>(List.of(etiquetas)))
                .build());
    }

    // ===== 2 administradores: gestionan el catálogo y el personal, no la operación diaria =====
    private void cargarAdmins() {
        adminRepository.save(Admin.builder()
                .nombre("Mono").email("mono@theroyale.com").password("admin123")
                .build());

        adminRepository.save(Admin.builder()
                .nombre("Mati").email("mati@theroyale.com").password("admin123")
                .build());
    }

    // ===== 5 tipos de habitación =====
    private void cargarTiposHabitacion() {
        tipoHabitacionRepository.save(TipoHabitacion.builder()
                .nombre("Normal")
                .descripcion("Elegant essentials for a refined stay. 1-2 guests, double bed, private bathroom, premium Wi-Fi.")
                .imagenUrl("/images/suite-3.webp")
                .build());

        tipoHabitacionRepository.save(TipoHabitacion.builder()
                .nombre("Executive")
                .descripcion("Designed for those who work while they travel. 1-2 guests, executive workspace, king bed, premium Wi-Fi.")
                .imagenUrl("/images/suite-1.webp")
                .build());

        tipoHabitacionRepository.save(TipoHabitacion.builder()
                .nombre("VIP")
                .descripcion("A private space to relax and unwind. Up to 3 guests, separate living area, premium amenities, city views.")
                .imagenUrl("/images/suite-2.webp")
                .build());

        tipoHabitacionRepository.save(TipoHabitacion.builder()
                .nombre("Luxury")
                .descripcion("The ultimate expression of The Royale. Up to 4 guests, full suite, jacuzzi, privileged city view.")
                .imagenUrl("/images/suite-4.webp")
                .build());

        tipoHabitacionRepository.save(TipoHabitacion.builder()
                .nombre("Presidential Suite")
                .descripcion("Unmatched exclusivity. Up to 6 guests, private terrace, butler service, panoramic city view, private dining.")
                .imagenUrl("/images/luxury 1.jpg")
                .build());
    }

    // ===== 10 clientes =====
    private void cargarClientes() {
        clienteRepository.save(Cliente.builder()
                .nombre("Carlos").apellido("Mendoza").cedula("1020304050").email("carlos.mendoza@email.com")
                .password("pass1234").telefono("+1-555-0101").fechaRegistro(LocalDate.of(2025, 1, 15))
                .build());

        clienteRepository.save(Cliente.builder()
                .nombre("Ana").apellido("García").cedula("1020304051").email("ana.garcia@email.com")
                .password("pass1234").telefono("+1-555-0102").fechaRegistro(LocalDate.of(2025, 2, 20))
                .build());

        clienteRepository.save(Cliente.builder()
                .nombre("Luis").apellido("Hernández").cedula("1020304052").email("luis.hernandez@email.com")
                .password("pass1234").telefono("+1-555-0103").fechaRegistro(LocalDate.of(2025, 3, 5))
                .build());

        clienteRepository.save(Cliente.builder()
                .nombre("Sofia").apellido("Torres").cedula("1020304053").email("sofia.torres@email.com")
                .password("pass1234").telefono("+1-555-0104").fechaRegistro(LocalDate.of(2025, 3, 18))
                .build());

        clienteRepository.save(Cliente.builder()
                .nombre("Miguel").apellido("Ramírez").cedula("1020304054").email("miguel.ramirez@email.com")
                .password("pass1234").telefono("+1-555-0105").fechaRegistro(LocalDate.of(2025, 4, 2))
                .build());

        clienteRepository.save(Cliente.builder()
                .nombre("Isabella").apellido("López").cedula("1020304055").email("isabella.lopez@email.com")
                .password("pass1234").telefono("+1-555-0106").fechaRegistro(LocalDate.of(2025, 4, 25))
                .build());

        clienteRepository.save(Cliente.builder()
                .nombre("Andrés").apellido("Martínez").cedula("1020304056").email("andres.martinez@email.com")
                .password("pass1234").telefono("+1-555-0107").fechaRegistro(LocalDate.of(2025, 5, 10))
                .build());

        clienteRepository.save(Cliente.builder()
                .nombre("Valentina").apellido("Rodríguez").cedula("1020304057").email("valentina.rodriguez@email.com")
                .password("pass1234").telefono("+1-555-0108").fechaRegistro(LocalDate.of(2025, 6, 1))
                .build());

        clienteRepository.save(Cliente.builder()
                .nombre("Jorge").apellido("Sánchez").cedula("1020304058").email("jorge.sanchez@email.com")
                .password("pass1234").telefono("+1-555-0109").fechaRegistro(LocalDate.of(2025, 7, 14))
                .build());

        clienteRepository.save(Cliente.builder()
                .nombre("Camila").apellido("Díaz").cedula("1020304059").email("camila.diaz@email.com")
                .password("pass1234").telefono("+1-555-0110").fechaRegistro(LocalDate.of(2025, 8, 22))
                .build());

        // ===== Huéspedes para probar el plan de visita (ver cargarReservasParaPlan) =====
        // Van al final para no mover los índices clientes.get(0..9) que usan las demás reservas.
        clienteRepository.save(Cliente.builder()
                .nombre("Daniel").apellido("Castro").cedula("1020304060").email("daniel.castro@email.com")
                .password("pass1234").telefono("+1-555-0111").fechaRegistro(LocalDate.of(2025, 9, 3))
                .build());

        clienteRepository.save(Cliente.builder()
                .nombre("Lucía").apellido("Moreno").cedula("1020304061").email("lucia.moreno@email.com")
                .password("pass1234").telefono("+1-555-0112").fechaRegistro(LocalDate.of(2025, 9, 18))
                .build());

        clienteRepository.save(Cliente.builder()
                .nombre("Mateo").apellido("Vargas").cedula("1020304062").email("mateo.vargas@email.com")
                .password("pass1234").telefono("+1-555-0113").fechaRegistro(LocalDate.of(2025, 10, 2))
                .build());

        clienteRepository.save(Cliente.builder()
                .nombre("Elena").apellido("Ruiz").cedula("1020304063").email("elena.ruiz@email.com")
                .password("pass1234").telefono("+1-555-0114").fechaRegistro(LocalDate.of(2025, 10, 20))
                .build());

        clienteRepository.save(Cliente.builder()
                .nombre("Tomás").apellido("Herrera").cedula("1020304064").email("tomas.herrera@email.com")
                .password("pass1234").telefono("+1-555-0115").fechaRegistro(LocalDate.of(2025, 11, 1))
                .build());
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
                "/images/spa.webp",
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
                List.of("/images/spa.webp", "/images/architecture.jpg", "/images/new_spa.jpg"));

        guardarServicio("Dining",
                "Experience exceptional cuisine without leaving The Royale. Our signature restaurant serves contemporary New York cuisine from breakfast through late-night dining. In-room service is available 24 hours a day.",
                60.0,
                "/images/dinningroom-1.webp",
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
                List.of("/images/dinningroom-1.webp", "/images/restaurant.jpg", "/images/new_dining.jpg"));

        guardarServicio("Business",
                "Stay productive from the heart of Manhattan. The Royale Business Center delivers everything corporate guests need - from state-of-the-art meeting technology to full executive support, all within steps of your room.",
                35.0,
                "/images/meeting-room.jpg",
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
                List.of("/images/meeting-room.jpg", "/images/skyline.jpg", "/images/new_lounge.jpg"));

        guardarServicio("Concierge",
                "Our multilingual concierge team is available around the clock to orchestrate every aspect of your New York experience. No request is too extraordinary - we specialise in turning the impossible into the unforgettable.",
                25.0,
                "/images/concierge.jpg",
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
                List.of("/images/concierge.jpg", "/images/broadway.jpg", "/images/new_concierge.jpg"));

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
                .precio(precio)
                .imagenUrl(imagenUrl)
                .caracteristicas(caracteristicas)
                .galeriaUrls(galeriaUrls)
                .build());
    }

    // ===== 5 operadores (personal del hotel; entran por /login y son redirigidos a /operator) =====
    private void cargarOperadores() {
        operadorRepository.save(Operador.builder()
                .nombre("Laura Gomez").email("laura.gomez@theroyale.com")
                .password("operator123").tipo(TipoOperador.OPERADOR)
                .build());

        operadorRepository.save(Operador.builder()
                .nombre("Pedro Ruiz").email("pedro.ruiz@theroyale.com")
                .password("operator123").tipo(TipoOperador.OPERADOR)
                .build());

        operadorRepository.save(Operador.builder()
                .nombre("Maria Castillo").email("maria.castillo@theroyale.com")
                .password("operator123").tipo(TipoOperador.OPERADOR)
                .build());

        operadorRepository.save(Operador.builder()
                .nombre("James Wright").email("james.wright@theroyale.com")
                .password("operator123").tipo(TipoOperador.OPERADOR)
                .build());

        operadorRepository.save(Operador.builder()
                .nombre("Sofia Chen").email("sofia.chen@theroyale.com")
                .password("operator123").tipo(TipoOperador.OPERADOR)
                .build());
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
}
