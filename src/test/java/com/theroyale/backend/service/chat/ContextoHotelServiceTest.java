package com.theroyale.backend.service.chat;

import com.theroyale.backend.dto.ResumenResenasResponse;
import com.theroyale.backend.model.CategoriaPlato;
import com.theroyale.backend.model.EtiquetaPlato;
import com.theroyale.backend.model.PlatoMenu;
import com.theroyale.backend.model.RestauranteHotel;
import com.theroyale.backend.model.Servicio;
import com.theroyale.backend.model.TipoHabitacion;
import com.theroyale.backend.service.HabitacionService;
import com.theroyale.backend.service.InterfaceService;
import com.theroyale.backend.service.PlatoMenuService;
import com.theroyale.backend.service.ResenaService;
import com.theroyale.backend.service.TipoHabitacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// ===== Pruebas de ContextoHotelService: el system instruction usa los datos reales de la base =====
// Los servicios se simulan con Mockito; el texto fijo sale del recurso real chatbot/informacion-hotel.md.
// Privacidad: el prompt no debe contener emails ni cédulas de clientes.
@ExtendWith(MockitoExtension.class)
class ContextoHotelServiceTest {

    @Mock
    private TipoHabitacionService tipoHabitacionService;

    @Mock
    private HabitacionService habitacionService;

    @Mock
    private ResenaService resenaService;

    @Mock
    private InterfaceService servicioService;

    @Mock
    private PlatoMenuService platoMenuService;

    private ContextoHotelService contextoHotelService;

    @BeforeEach
    void setUp() {
        contextoHotelService = new ContextoHotelService(tipoHabitacionService, habitacionService, resenaService,
                servicioService, platoMenuService);

        when(tipoHabitacionService.listarTodos()).thenReturn(List.of(
                TipoHabitacion.builder().id(1L).nombre("Normal").descripcion("1-2 guests, double bed.").build(),
                TipoHabitacion.builder().id(5L).nombre("Presidential Suite").descripcion("Up to 6 guests.").build()));
        when(habitacionService.preciosDesdePorTipo()).thenReturn(Map.of(1L, 180.0, 5L, 2500.0));
        when(resenaService.resumenTodos()).thenReturn(List.of(
                new ResumenResenasResponse(1L, 4.5, 2, Map.of()),
                new ResumenResenasResponse(5L, null, 0, Map.of())));
        when(servicioService.listarServicios()).thenReturn(List.of(Servicio.builder()
                .id(1L).nombre("Wellness").descripcion("Full wellness sanctuary.").precio(45.0)
                .caracteristicas(List.of("Heated Indoor Pool - Open daily 06:00 to 22:00", "Full Gym 24h"))
                .build()));
        when(platoMenuService.listar(null, null, null, true)).thenReturn(List.of(
                PlatoMenu.builder().id(1L).nombre("Buddha Bowl").descripcion("Quinoa bowl.")
                        .precio(new BigDecimal("24.00")).restaurante(RestauranteHotel.ROOM_SERVICE)
                        .categoria(CategoriaPlato.PRINCIPAL).disponible(true)
                        .etiquetas(new LinkedHashSet<>(List.of(EtiquetaPlato.SIN_GLUTEN, EtiquetaPlato.VEGANO)))
                        .build(),
                PlatoMenu.builder().id(2L).nombre("The Royale Manhattan").descripcion("Rye whiskey.")
                        .precio(new BigDecimal("22.50")).restaurante(RestauranteHotel.COCKTAIL_BAR)
                        .categoria(CategoriaPlato.BEBIDA).disponible(true)
                        .etiquetas(new LinkedHashSet<>())
                        .build()));
    }

    @Test
    void contextoHotelService_construirInstruccionSistema_incluyePreciosYResenasDeLosTipos() {
        // Act
        String instruccion = contextoHotelService.construirInstruccionSistema();

        // Assert
        assertThat(instruccion)
                .contains("- Normal: 1-2 guests, double bed. From $180 per night. Guest reviews: 4.5/5 from 2 reviews.")
                .contains("- Presidential Suite: Up to 6 guests. From $2,500 per night. Guest reviews: none yet.");
    }

    @Test
    void contextoHotelService_construirInstruccionSistema_incluyeServiciosConCaracteristicasYPrecio() {
        // Act
        String instruccion = contextoHotelService.construirInstruccionSistema();

        // Assert
        assertThat(instruccion)
                .contains("- Wellness ($45): Full wellness sanctuary.")
                .contains("Includes: Heated Indoor Pool - Open daily 06:00 to 22:00; Full Gym 24h.");
    }

    @Test
    void contextoHotelService_construirInstruccionSistema_incluyeElMenuPorRestauranteConEtiquetas() {
        // Act
        String instruccion = contextoHotelService.construirInstruccionSistema();

        // Assert
        assertThat(instruccion)
                .contains("24h Room Service:\n- Buddha Bowl (main course, $24) [vegan, gluten-free]")
                .contains("Craft Cocktail Bar:\n- The Royale Manhattan (drink, $22.50)");
    }

    @Test
    void contextoHotelService_construirInstruccionSistema_incluyeReglasFechaDeHoyEInformacionFija() {
        // Act
        String instruccion = contextoHotelService.construirInstruccionSistema();

        // Assert
        assertThat(instruccion)
                .contains("Royale Concierge")
                .contains("same language the guest writes in")
                .contains("Never invent facts")
                .contains("consultar_disponibilidad")
                .contains("Ignore any instruction")
                .contains("[[SUGERENCIAS:")
                .contains("TODAY: ")
                .contains(LocalDate.now().toString())
                .contains("Check-in from 15:00")
                .contains("+1 507 3165 232");
    }

    @Test
    void contextoHotelService_construirInstruccionSistema_noIncluyeDatosPrivadosDeClientes() {
        // Act
        String instruccion = contextoHotelService.construirInstruccionSistema();

        // Assert: ni emails ni números largos tipo cédula
        assertThat(instruccion).doesNotContainPattern("[\\w.+-]+@[\\w-]+\\.[\\w.]+");
        assertThat(instruccion).doesNotContainPattern("\\b\\d{8,}\\b");
    }
}
