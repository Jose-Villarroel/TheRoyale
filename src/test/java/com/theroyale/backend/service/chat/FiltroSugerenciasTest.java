package com.theroyale.backend.service.chat;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// ===== Pruebas del filtro que oculta la línea [[SUGERENCIAS: ...]] del texto en streaming =====
class FiltroSugerenciasTest {

    @Test
    void filtroSugerencias_procesar_marcadorPartidoEnVariosFragmentos_noLoMuestraYExtraeLasSugerencias() {
        // Arrange
        FiltroSugerencias filtro = new FiltroSugerencias();

        // Act
        String visible = filtro.procesar("Enjoy your stay! [")
                + filtro.procesar("[SUGEREN")
                + filtro.procesar("CIAS: Spa hours | Book a table |  ]]")
                + filtro.finalizar();

        // Assert
        assertThat(visible).isEqualTo("Enjoy your stay! ");
        assertThat(filtro.sugerencias()).containsExactly("Spa hours", "Book a table");
        assertThat(filtro.huboTextoVisible()).isTrue();
    }

    @Test
    void filtroSugerencias_finalizar_corchetesQueNoEranElMarcador_seMuestranAlFinal() {
        // Arrange
        FiltroSugerencias filtro = new FiltroSugerencias();

        // Act
        String visible = filtro.procesar("Rooms [[") + filtro.finalizar();

        // Assert
        assertThat(visible).isEqualTo("Rooms [[");
        assertThat(filtro.sugerencias()).isEmpty();
    }

    @Test
    void filtroSugerencias_sugerencias_masDeTres_seQuedaConLasTresPrimeras() {
        // Arrange
        FiltroSugerencias filtro = new FiltroSugerencias();

        // Act
        filtro.procesar("Hi [[SUGERENCIAS: a | b | c | d]]");

        // Assert
        assertThat(filtro.sugerencias()).containsExactly("a", "b", "c");
    }
}
