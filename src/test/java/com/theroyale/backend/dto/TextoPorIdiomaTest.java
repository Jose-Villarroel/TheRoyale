package com.theroyale.backend.dto;

import com.theroyale.backend.model.PlatoMenu;
import com.theroyale.backend.model.Servicio;
import com.theroyale.backend.model.TipoHabitacion;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

// ===== El contenido de la base sale en el idioma pedido y cae al inglés cuando falta =====
// El inglés es el idioma base: nombre / descripcion siempre tienen valor y nombreEs / descripcionEs
// son opcionales (hoy solo los siembra DataLoader). Siguiendo docs/i18n-glosario.md, los nombres de
// tipo de habitación y de servicio se quedan sin versión en español a propósito.
class TextoPorIdiomaTest {

    private static final Locale ESPANOL = Locale.forLanguageTag("es");

    @Test
    void textoPorIdioma_elegir_conEspanolDisponible_devuelveElEspanolSoloParaEseIdioma() {
        // Act & Assert
        assertThat(TextoPorIdioma.elegir(ESPANOL, "Stay", "Estadía")).isEqualTo("Estadía");
        assertThat(TextoPorIdioma.elegir(Locale.forLanguageTag("es-CO"), "Stay", "Estadía")).isEqualTo("Estadía");
        assertThat(TextoPorIdioma.elegir(Locale.ENGLISH, "Stay", "Estadía")).isEqualTo("Stay");
        assertThat(TextoPorIdioma.elegir(Locale.FRENCH, "Stay", "Estadía")).isEqualTo("Stay");
        assertThat(TextoPorIdioma.elegir(null, "Stay", "Estadía")).isEqualTo("Stay");
    }

    // Un valor nulo o en blanco cuenta como ausente: una cadena vacía guardada por error no debe
    // dejar la tarjeta del huésped sin texto.
    @Test
    void textoPorIdioma_elegir_sinEspanolOEnBlanco_caeAlIngles() {
        // Act & Assert
        assertThat(TextoPorIdioma.elegir(ESPANOL, "Stay", null)).isEqualTo("Stay");
        assertThat(TextoPorIdioma.elegir(ESPANOL, "Stay", "   ")).isEqualTo("Stay");
    }

    @Test
    void tipoHabitacionResponseDto_from_enEspanol_traduceLaDescripcionYConservaElNombre() {
        // Arrange: el nombre del tipo no se traduce (nombreEs queda null, como en DataLoader)
        TipoHabitacion tipo = TipoHabitacion.builder()
                .id(5L)
                .nombre("Presidential Suite")
                .descripcion("Unmatched exclusivity.")
                .descripcionEs("Exclusividad sin igual.")
                .imagenUrl("/images/suite.jpg")
                .build();

        // Act & Assert
        assertThat(TipoHabitacionResponseDto.from(tipo, ESPANOL).nombre()).isEqualTo("Presidential Suite");
        assertThat(TipoHabitacionResponseDto.from(tipo, ESPANOL).descripcion()).isEqualTo("Exclusividad sin igual.");
        assertThat(TipoHabitacionResponseDto.from(tipo, Locale.ENGLISH).descripcion()).isEqualTo("Unmatched exclusivity.");
    }

    @Test
    void servicioResponseDto_from_sinDescripcionEs_caeAlIngles() {
        // Arrange
        Servicio servicio = Servicio.builder()
                .id(1L)
                .nombre("Wellness")
                .descripcion("Full wellness sanctuary.")
                .precio(45.0)
                .imagenUrl("/images/spa.jpg")
                .caracteristicas(List.of())
                .galeriaUrls(List.of())
                .build();

        // Act & Assert
        assertThat(ServicioResponseDto.from(servicio, ESPANOL).nombre()).isEqualTo("Wellness");
        assertThat(ServicioResponseDto.from(servicio, ESPANOL).descripcion()).isEqualTo("Full wellness sanctuary.");
    }

    @Test
    void platoMenuResponse_from_conNombreYDescripcionEs_devuelveLosDos() {
        // Arrange
        PlatoMenu plato = PlatoMenu.builder()
                .id(9L)
                .nombre("Chicken Caesar Salad")
                .descripcion("Grilled chicken and romaine hearts.")
                .nombreEs("Ensalada César con pollo")
                .descripcionEs("Pollo a la parrilla y corazones de lechuga romana.")
                .precio(new BigDecimal("22.00"))
                .etiquetas(new LinkedHashSet<>())
                .build();

        // Act & Assert
        assertThat(PlatoMenuResponse.from(plato, ESPANOL).nombre()).isEqualTo("Ensalada César con pollo");
        assertThat(PlatoMenuResponse.from(plato, Locale.ENGLISH).nombre()).isEqualTo("Chicken Caesar Salad");
        assertThat(PlatoMenuResponse.from(plato, Locale.ENGLISH).descripcion())
                .isEqualTo("Grilled chicken and romaine hearts.");
    }
}
