package com.theroyale.backend.service.chat;

import com.theroyale.backend.dto.DisponibilidadTipoResponse;
import com.theroyale.backend.dto.PlatoMenuResponse;
import com.theroyale.backend.dto.ResumenResenasResponse;
import com.theroyale.backend.dto.TarjetaHabitacionChat;
import com.theroyale.backend.dto.TarjetasHabitacionChatResponse;
import com.theroyale.backend.dto.TarjetasPlatoChatResponse;
import com.theroyale.backend.model.CategoriaPlato;
import com.theroyale.backend.model.EtiquetaPlato;
import com.theroyale.backend.model.PlatoMenu;
import com.theroyale.backend.model.RestauranteHotel;
import com.theroyale.backend.service.PlatoMenuService;
import com.theroyale.backend.service.ResenaService;
import com.theroyale.backend.service.ReservaService;
import com.theroyale.backend.service.chat.GeminiClient.LlamadaFuncion;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// ===== Herramientas (function calling) que Gemini puede pedir; todas son de solo lectura =====
// consultar_disponibilidad -> ReservaService.consultarDisponibilidad + resumen de reseñas
// buscar_platos            -> PlatoMenuService (solo platos disponibles)
// Cada ejecución devuelve: la respuesta para el modelo (functionResponse) y, si hay resultados,
// las tarjetas que el chat muestra al cliente. Un argumento inválido (fecha pasada, formato malo)
// NO es una excepción: se devuelve {"error": "..."} al modelo para que lo explique con amabilidad.
// Usa inyección por constructor para poder probar ChatService con esta clase real y servicios simulados.
@Service
public class HerramientasChatService {

    public static final String CONSULTAR_DISPONIBILIDAD = "consultar_disponibilidad";
    public static final String BUSCAR_PLATOS = "buscar_platos";

    // Más tarjetas de platos saturan el panel del chat; el modelo recibe la lista completa
    static final int MAXIMO_TARJETAS_PLATOS = 8;

    // "1-2 guests", "Up to 4 guests": la capacidad sale de la descripción del tipo
    private static final Pattern CAPACIDAD_RANGO = Pattern.compile("(\\d+)\\s*-\\s*(\\d+)\\s+guests", Pattern.CASE_INSENSITIVE);
    private static final Pattern CAPACIDAD_HASTA = Pattern.compile("up to\\s+(\\d+)\\s+guests", Pattern.CASE_INSENSITIVE);

    private final ReservaService reservaService;
    private final ResenaService resenaService;
    private final PlatoMenuService platoMenuService;

    public HerramientasChatService(ReservaService reservaService, ResenaService resenaService,
                                   PlatoMenuService platoMenuService) {
        this.reservaService = reservaService;
        this.resenaService = resenaService;
        this.platoMenuService = platoMenuService;
    }

    // respuesta: lo que lee el modelo; tarjetas: TarjetasHabitacionChatResponse, TarjetasPlatoChatResponse o null
    public record ResultadoHerramienta(Map<String, Object> respuesta, Object tarjetas) {
    }

    // ===== Declaraciones (functionDeclarations) en el formato de esquema de Gemini =====
    public List<Map<String, Object>> declaraciones() {
        Map<String, Object> disponibilidad = Map.of(
                "name", CONSULTAR_DISPONIBILIDAD,
                "description", "Checks the real availability and nightly prices of The Royale's room types for a stay. "
                        + "Call it whenever the guest asks about availability, prices for specific dates or wants to book.",
                "parameters", Map.of(
                        "type", "OBJECT",
                        "properties", Map.of(
                                "fechaInicio", Map.of("type", "STRING",
                                        "description", "Check-in date in yyyy-MM-dd format."),
                                "fechaFin", Map.of("type", "STRING",
                                        "description", "Check-out date in yyyy-MM-dd format (after the check-in date)."),
                                "huespedes", Map.of("type", "INTEGER",
                                        "description", "Number of guests, if the guest mentioned it.")),
                        "required", List.of("fechaInicio", "fechaFin")));

        Map<String, Object> platos = Map.of(
                "name", BUSCAR_PLATOS,
                "description", "Searches the available dishes of the hotel's restaurants. All filters are optional. "
                        + "Call it whenever the guest asks about food, drinks, the menu or dietary options.",
                "parameters", Map.of(
                        "type", "OBJECT",
                        "properties", Map.of(
                                "restaurante", Map.of("type", "STRING", "enum", nombres(RestauranteHotel.values()),
                                        "description", "Restaurant or outlet."),
                                "categoria", Map.of("type", "STRING", "enum", nombres(CategoriaPlato.values()),
                                        "description", "ENTRADA = starter, PRINCIPAL = main course, POSTRE = dessert, "
                                                + "BEBIDA = drink, DESAYUNO = breakfast."),
                                "etiqueta", Map.of("type", "STRING", "enum", nombres(EtiquetaPlato.values()),
                                        "description", "Dietary tag: VEGANO = vegan, VEGETARIANO = vegetarian, "
                                                + "SIN_GLUTEN = gluten-free, PICANTE = spicy."))));

        return List.of(disponibilidad, platos);
    }

    public ResultadoHerramienta ejecutar(LlamadaFuncion llamada) {
        Map<String, Object> argumentos = llamada.argumentos() == null ? Map.of() : llamada.argumentos();
        try {
            return switch (llamada.nombre()) {
                case CONSULTAR_DISPONIBILIDAD -> consultarDisponibilidad(argumentos);
                case BUSCAR_PLATOS -> buscarPlatos(argumentos);
                default -> error("Unknown tool: " + llamada.nombre());
            };
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return error(ex.getMessage());
        }
    }

    // ===== consultar_disponibilidad =====
    private ResultadoHerramienta consultarDisponibilidad(Map<String, Object> argumentos) {
        LocalDate inicio = leerFecha(argumentos.get("fechaInicio"), "fechaInicio");
        LocalDate fin = leerFecha(argumentos.get("fechaFin"), "fechaFin");
        Integer huespedes = leerEntero(argumentos.get("huespedes"));
        if (huespedes != null && huespedes < 1) {
            throw new IllegalArgumentException("The number of guests must be at least 1.");
        }

        // Valida fechas pasadas y fin <= inicio con los mismos mensajes que la reserva
        List<DisponibilidadTipoResponse> disponibilidad = reservaService.consultarDisponibilidad(inicio, fin);
        Map<Long, ResumenResenasResponse> resenasPorTipo = resenaService.resumenTodos().stream()
                .collect(Collectors.toMap(ResumenResenasResponse::tipoHabitacionId, Function.identity(), (a, b) -> a));

        List<TarjetaHabitacionChat> tarjetas = new ArrayList<>();
        List<Map<String, Object>> habitacionesParaModelo = new ArrayList<>();
        for (DisponibilidadTipoResponse tipo : disponibilidad) {
            Integer capacidad = capacidadMaxima(tipo.descripcion());
            if (tipo.habitacionesDisponibles() <= 0
                    || (huespedes != null && capacidad != null && capacidad < huespedes)) {
                continue;
            }
            ResumenResenasResponse resenas = resenasPorTipo.get(tipo.tipoHabitacionId());
            Double promedio = resenas == null ? null : resenas.promedio();
            long totalResenas = resenas == null ? 0 : resenas.total();

            tarjetas.add(new TarjetaHabitacionChat(tipo.tipoHabitacionId(), tipo.nombre(), tipo.descripcion(),
                    tipo.imagenUrl(), tipo.precioDesde(), tipo.habitacionesDisponibles(), promedio, totalResenas,
                    inicio, fin));

            Map<String, Object> resumen = new LinkedHashMap<>();
            resumen.put("tipo", tipo.nombre());
            resumen.put("habitacionesLibres", tipo.habitacionesDisponibles());
            resumen.put("precioDesdePorNocheUSD", tipo.precioDesde());
            resumen.put("capacidadMaxima", capacidad);
            resumen.put("promedioResenas", promedio);
            resumen.put("totalResenas", totalResenas);
            habitacionesParaModelo.add(resumen);
        }

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("fechaInicio", inicio.toString());
        respuesta.put("fechaFin", fin.toString());
        respuesta.put("noches", ChronoUnit.DAYS.between(inicio, fin));
        respuesta.put("huespedes", huespedes);
        respuesta.put("habitacionesDisponibles", habitacionesParaModelo);
        if (tarjetas.isEmpty()) {
            respuesta.put("nota", "No room type is available for these dates"
                    + (huespedes == null ? "" : " and number of guests")
                    + ". Suggest other dates or contacting the front desk.");
            return new ResultadoHerramienta(respuesta, null);
        }
        respuesta.put("nota", "The guest sees these rooms as cards with 'View room' and 'Book this room' buttons.");
        return new ResultadoHerramienta(respuesta, TarjetasHabitacionChatResponse.de(tarjetas));
    }

    // ===== buscar_platos =====
    private ResultadoHerramienta buscarPlatos(Map<String, Object> argumentos) {
        RestauranteHotel restaurante = leerEnum(argumentos.get("restaurante"), RestauranteHotel.class, "restaurante");
        CategoriaPlato categoria = leerEnum(argumentos.get("categoria"), CategoriaPlato.class, "categoria");
        EtiquetaPlato etiqueta = leerEnum(argumentos.get("etiqueta"), EtiquetaPlato.class, "etiqueta");

        List<PlatoMenu> platos = platoMenuService.listar(restaurante, categoria, etiqueta, true);

        List<Map<String, Object>> platosParaModelo = platos.stream().map(plato -> {
            Map<String, Object> resumen = new LinkedHashMap<>();
            resumen.put("nombre", plato.getNombre());
            resumen.put("descripcion", plato.getDescripcion());
            resumen.put("precioUSD", plato.getPrecio());
            resumen.put("restaurante", plato.getRestaurante().name());
            resumen.put("categoria", plato.getCategoria().name());
            resumen.put("etiquetas", plato.getEtiquetas().stream().map(Enum::name).sorted().toList());
            return resumen;
        }).toList();

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("total", platos.size());
        respuesta.put("platos", platosParaModelo);
        if (platos.isEmpty()) {
            respuesta.put("nota", "No available dish matches these filters.");
            return new ResultadoHerramienta(respuesta, null);
        }
        if (platos.size() > MAXIMO_TARJETAS_PLATOS) {
            respuesta.put("nota", "Only the first " + MAXIMO_TARJETAS_PLATOS + " dishes are shown as cards.");
        }
        List<PlatoMenuResponse> tarjetas = platos.stream()
                .limit(MAXIMO_TARJETAS_PLATOS)
                .map(PlatoMenuResponse::from)
                .toList();
        return new ResultadoHerramienta(respuesta, TarjetasPlatoChatResponse.de(tarjetas));
    }

    // ===== Lectura de argumentos (Gemini los manda como JSON: textos y números) =====
    private LocalDate leerFecha(Object valor, String nombre) {
        if (valor == null || valor.toString().isBlank()) {
            throw new IllegalArgumentException("The " + nombre + " date is required (yyyy-MM-dd).");
        }
        try {
            return LocalDate.parse(valor.toString().trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid " + nombre + " date: " + valor + ". Use the yyyy-MM-dd format.");
        }
    }

    private Integer leerEntero(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof Number numero) {
            return numero.intValue();
        }
        try {
            return (int) Double.parseDouble(valor.toString().trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid number of guests: " + valor);
        }
    }

    private <E extends Enum<E>> E leerEnum(Object valor, Class<E> tipo, String nombre) {
        if (valor == null || valor.toString().isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(tipo, valor.toString().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid " + nombre + ": " + valor + ". Valid values: "
                    + nombres(tipo.getEnumConstants()));
        }
    }

    static Integer capacidadMaxima(String descripcion) {
        if (descripcion == null) {
            return null;
        }
        Matcher rango = CAPACIDAD_RANGO.matcher(descripcion);
        if (rango.find()) {
            return Integer.parseInt(rango.group(2));
        }
        Matcher hasta = CAPACIDAD_HASTA.matcher(descripcion);
        if (hasta.find()) {
            return Integer.parseInt(hasta.group(1));
        }
        return null;
    }

    private static List<String> nombres(Enum<?>[] valores) {
        return Arrays.stream(valores).map(Enum::name).toList();
    }

    private static ResultadoHerramienta error(String mensaje) {
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("error", mensaje);
        return new ResultadoHerramienta(respuesta, null);
    }
}
