package com.theroyale.backend.service;

import com.theroyale.backend.dto.PlatoMenuRequest;
import com.theroyale.backend.errors.RecursoNoEncontradoException;
import com.theroyale.backend.model.CategoriaPlato;
import com.theroyale.backend.model.EtiquetaPlato;
import com.theroyale.backend.model.PlatoMenu;
import com.theroyale.backend.model.RestauranteHotel;
import com.theroyale.backend.repository.PlatoMenuRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// ===== Pruebas de PlatoMenuService con mocks =====
// Validaciones (nombre, descripción, precio > 0, restaurante y categoría), filtros y el reemplazo
// de etiquetas al editar. La consulta con filtros se prueba contra H2 en PlatoMenuRepositoryTest.
@ExtendWith(MockitoExtension.class)
class PlatoMenuServiceTest {

    @Mock
    private PlatoMenuRepository platoMenuRepository;

    @InjectMocks
    private PlatoMenuService platoMenuService;

    // ===== Listar: pasa los filtros tal cual al repositorio =====
    @Test
    void platoMenuService_listar_conFiltros_delegaEnElRepositorio() {
        // Arrange
        PlatoMenu bowl = crearPlato(3L, "Buddha Bowl");
        when(platoMenuRepository.buscar(RestauranteHotel.ROOM_SERVICE, null, EtiquetaPlato.VEGANO, true))
                .thenReturn(List.of(bowl));

        // Act
        List<PlatoMenu> platos = platoMenuService.listar(RestauranteHotel.ROOM_SERVICE, null, EtiquetaPlato.VEGANO, true);

        // Assert
        assertThat(platos).containsExactly(bowl);
    }

    @Test
    void platoMenuService_obtenerPorId_inexistente_lanzaRecursoNoEncontrado() {
        // Arrange
        when(platoMenuRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> platoMenuService.obtenerPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("99");
    }

    // ===== Crear =====
    @Test
    void platoMenuService_crear_datosValidos_guardaConTextosRecortadosYDisponiblePorDefecto() {
        // Arrange
        PlatoMenuRequest solicitud = new PlatoMenuRequest("  Avocado Toast ", " Smashed avocado on sourdough. ",
                new BigDecimal("19.00"), RestauranteHotel.BREAKFAST_LOUNGE, CategoriaPlato.DESAYUNO,
                List.of(EtiquetaPlato.VEGANO), null, "  ");
        when(platoMenuRepository.save(any(PlatoMenu.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        // Act
        PlatoMenu creado = platoMenuService.crear(solicitud);

        // Assert
        assertThat(creado.getNombre()).isEqualTo("Avocado Toast");
        assertThat(creado.getDescripcion()).isEqualTo("Smashed avocado on sourdough.");
        assertThat(creado.getPrecio()).isEqualByComparingTo("19.00");
        assertThat(creado.getEtiquetas()).containsExactly(EtiquetaPlato.VEGANO);
        assertThat(creado.isDisponible()).isTrue();
        assertThat(creado.getImagenUrl()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-5.50"})
    void platoMenuService_crear_precioNoPositivo_lanzaIllegalArgument(String precio) {
        // Arrange
        PlatoMenuRequest solicitud = solicitudValida(new BigDecimal(precio));

        // Act & Assert
        assertThatThrownBy(() -> platoMenuService.crear(solicitud))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than 0");
        verify(platoMenuRepository, never()).save(any());
    }

    @Test
    void platoMenuService_crear_sinNombre_lanzaIllegalArgument() {
        // Arrange
        PlatoMenuRequest solicitud = new PlatoMenuRequest("  ", "Descripcion", BigDecimal.TEN,
                RestauranteHotel.COCKTAIL_BAR, CategoriaPlato.BEBIDA, List.of(), true, null);

        // Act & Assert
        assertThatThrownBy(() -> platoMenuService.crear(solicitud))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name is required");
    }

    @Test
    void platoMenuService_crear_nombreDeMasDe100Caracteres_lanzaIllegalArgument() {
        // Arrange
        PlatoMenuRequest solicitud = new PlatoMenuRequest("x".repeat(101), "Descripcion", BigDecimal.TEN,
                RestauranteHotel.COCKTAIL_BAR, CategoriaPlato.BEBIDA, List.of(), true, null);

        // Act & Assert
        assertThatThrownBy(() -> platoMenuService.crear(solicitud))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("100");
    }

    @Test
    void platoMenuService_crear_descripcionDeMasDe500Caracteres_lanzaIllegalArgument() {
        // Arrange
        PlatoMenuRequest solicitud = new PlatoMenuRequest("Garden Mocktail", "x".repeat(501), BigDecimal.TEN,
                RestauranteHotel.COCKTAIL_BAR, CategoriaPlato.BEBIDA, List.of(), true, null);

        // Act & Assert
        assertThatThrownBy(() -> platoMenuService.crear(solicitud))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("500");
    }

    @Test
    void platoMenuService_crear_sinRestaurante_lanzaIllegalArgument() {
        // Arrange
        PlatoMenuRequest solicitud = new PlatoMenuRequest("Garden Mocktail", "Descripcion", BigDecimal.TEN,
                null, CategoriaPlato.BEBIDA, List.of(), true, null);

        // Act & Assert
        assertThatThrownBy(() -> platoMenuService.crear(solicitud))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Restaurant is required");
    }

    @Test
    void platoMenuService_crear_sinCategoria_lanzaIllegalArgument() {
        // Arrange
        PlatoMenuRequest solicitud = new PlatoMenuRequest("Garden Mocktail", "Descripcion", BigDecimal.TEN,
                RestauranteHotel.COCKTAIL_BAR, null, List.of(), true, null);

        // Act & Assert
        assertThatThrownBy(() -> platoMenuService.crear(solicitud))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category is required");
    }

    // ===== Actualizar =====
    @Test
    void platoMenuService_actualizar_reemplazaEtiquetasSobreLaEntidadExistente() {
        // Arrange
        PlatoMenu existente = crearPlato(5L, "Wagyu Sliders");
        existente.getEtiquetas().add(EtiquetaPlato.PICANTE);
        when(platoMenuRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(platoMenuRepository.save(any(PlatoMenu.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        PlatoMenuRequest solicitud = new PlatoMenuRequest("Veggie Sliders", "Three mini plant-based burgers.",
                new BigDecimal("21.00"), RestauranteHotel.COCKTAIL_BAR, CategoriaPlato.ENTRADA,
                List.of(EtiquetaPlato.VEGANO, EtiquetaPlato.VEGETARIANO), false, "/images/sliders.jpg");

        // Act
        PlatoMenu actualizado = platoMenuService.actualizar(5L, solicitud);

        // Assert
        assertThat(actualizado).isSameAs(existente);
        assertThat(actualizado.getId()).isEqualTo(5L);
        assertThat(actualizado.getNombre()).isEqualTo("Veggie Sliders");
        assertThat(actualizado.getEtiquetas()).containsExactlyInAnyOrder(EtiquetaPlato.VEGANO, EtiquetaPlato.VEGETARIANO);
        assertThat(actualizado.isDisponible()).isFalse();
        assertThat(actualizado.getImagenUrl()).isEqualTo("/images/sliders.jpg");
    }

    @Test
    void platoMenuService_actualizar_inexistente_lanzaRecursoNoEncontrado() {
        // Arrange
        when(platoMenuRepository.findById(42L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> platoMenuService.actualizar(42L, solicitudValida(BigDecimal.TEN)))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(platoMenuRepository, never()).save(any());
    }

    // ===== Eliminar =====
    @Test
    void platoMenuService_eliminar_existente_borraPorId() {
        // Arrange
        when(platoMenuRepository.findById(7L)).thenReturn(Optional.of(crearPlato(7L, "Acai Bowl")));

        // Act
        platoMenuService.eliminar(7L);

        // Assert
        verify(platoMenuRepository).deleteById(7L);
    }

    @Test
    void platoMenuService_eliminar_inexistente_noBorraNada() {
        // Arrange
        when(platoMenuRepository.findById(7L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> platoMenuService.eliminar(7L)).isInstanceOf(RecursoNoEncontradoException.class);
        verify(platoMenuRepository, never()).deleteById(any());
    }

    private PlatoMenuRequest solicitudValida(BigDecimal precio) {
        return new PlatoMenuRequest("Garden Mocktail", "Cucumber, mint and lime.", precio,
                RestauranteHotel.COCKTAIL_BAR, CategoriaPlato.BEBIDA, new ArrayList<>(), true, null);
    }

    private PlatoMenu crearPlato(Long id, String nombre) {
        return PlatoMenu.builder()
                .id(id)
                .nombre(nombre)
                .descripcion("Descripcion")
                .precio(new BigDecimal("20.00"))
                .restaurante(RestauranteHotel.ROOM_SERVICE)
                .categoria(CategoriaPlato.PRINCIPAL)
                .disponible(true)
                .etiquetas(new LinkedHashSet<>())
                .build();
    }
}
