package com.theroyale.backend.repository;

import com.theroyale.backend.model.CategoriaPlato;
import com.theroyale.backend.model.EtiquetaPlato;
import com.theroyale.backend.model.PlatoMenu;
import com.theroyale.backend.model.RestauranteHotel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// ===== Pruebas de la consulta con filtros opcionales de PlatoMenuRepository contra H2 =====
// Cada filtro en null se ignora; la etiqueta se busca dentro de la @ElementCollection y el
// resultado sale ordenado por restaurante, categoría y nombre, sin duplicados.
@DataJpaTest
@ActiveProfiles("test")
class PlatoMenuRepositoryTest {

    @Autowired
    private PlatoMenuRepository platoMenuRepository;

    @BeforeEach
    void setUp() {
        guardar("Wagyu Sliders", RestauranteHotel.COCKTAIL_BAR, CategoriaPlato.ENTRADA, true);
        guardar("Buddha Bowl", RestauranteHotel.ROOM_SERVICE, CategoriaPlato.PRINCIPAL, true,
                EtiquetaPlato.VEGANO, EtiquetaPlato.SIN_GLUTEN);
        guardar("Avocado Toast", RestauranteHotel.BREAKFAST_LOUNGE, CategoriaPlato.DESAYUNO, true,
                EtiquetaPlato.VEGANO);
        guardar("Lobster Linguine", RestauranteHotel.SIGNATURE_RESTAURANT, CategoriaPlato.PRINCIPAL, false,
                EtiquetaPlato.PICANTE);
        guardar("Beet Carpaccio", RestauranteHotel.SIGNATURE_RESTAURANT, CategoriaPlato.ENTRADA, true,
                EtiquetaPlato.VEGANO, EtiquetaPlato.SIN_GLUTEN);
    }

    @Test
    void platoMenuRepository_buscar_sinFiltros_devuelveTodosOrdenados() {
        // Act
        List<PlatoMenu> platos = platoMenuRepository.buscar(null, null, null, false);

        // Assert: los enums se guardan como texto, así que se ordenan alfabéticamente por su nombre
        assertThat(platos).extracting(PlatoMenu::getNombre).containsExactly(
                "Avocado Toast", "Wagyu Sliders", "Buddha Bowl", "Beet Carpaccio", "Lobster Linguine");
    }

    @Test
    void platoMenuRepository_buscar_porEtiqueta_noDuplicaPlatosConVariasEtiquetas() {
        // Act
        List<PlatoMenu> veganos = platoMenuRepository.buscar(null, null, EtiquetaPlato.VEGANO, false);

        // Assert
        assertThat(veganos).extracting(PlatoMenu::getNombre)
                .containsExactly("Avocado Toast", "Buddha Bowl", "Beet Carpaccio");
    }

    @Test
    void platoMenuRepository_buscar_combinandoFiltros_aplicaTodos() {
        // Act
        List<PlatoMenu> platos = platoMenuRepository.buscar(
                RestauranteHotel.SIGNATURE_RESTAURANT, CategoriaPlato.ENTRADA, EtiquetaPlato.SIN_GLUTEN, false);

        // Assert
        assertThat(platos).extracting(PlatoMenu::getNombre).containsExactly("Beet Carpaccio");
    }

    @Test
    void platoMenuRepository_buscar_soloDisponibles_excluyeLosNoDisponibles() {
        // Act
        List<PlatoMenu> platos = platoMenuRepository.buscar(RestauranteHotel.SIGNATURE_RESTAURANT, null, null, true);

        // Assert
        assertThat(platos).extracting(PlatoMenu::getNombre).containsExactly("Beet Carpaccio");
    }

    private void guardar(String nombre, RestauranteHotel restaurante, CategoriaPlato categoria, boolean disponible,
                         EtiquetaPlato... etiquetas) {
        platoMenuRepository.save(PlatoMenu.builder()
                .nombre(nombre)
                .descripcion("Descripcion de " + nombre)
                .precio(new BigDecimal("20.00"))
                .restaurante(restaurante)
                .categoria(categoria)
                .disponible(disponible)
                .etiquetas(new LinkedHashSet<>(List.of(etiquetas)))
                .build());
    }
}
