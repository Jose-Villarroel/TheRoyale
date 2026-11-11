package com.theroyale.backend.repository;

import com.theroyale.backend.model.Cliente;
import com.theroyale.backend.model.EstadoHabitacion;
import com.theroyale.backend.model.EstadoReserva;
import com.theroyale.backend.model.Habitacion;
import com.theroyale.backend.model.Resena;
import com.theroyale.backend.model.Reserva;
import com.theroyale.backend.model.TipoHabitacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// ===== Pruebas de las consultas de ResenaRepository contra H2 =====
// Lo central: los agregados públicos (contarPorCalificacion y resumenPorTipo) ignoran las reseñas
// ocultas (RN-8), y la base rechaza una segunda reseña para la misma reserva (RN-3).
@DataJpaTest
@ActiveProfiles("test")
class ResenaRepositoryTest {

    @Autowired
    private ResenaRepository resenaRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private HabitacionRepository habitacionRepository;

    @Autowired
    private TipoHabitacionRepository tipoHabitacionRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    private TipoHabitacion suite;
    private TipoHabitacion normal;
    private Habitacion habitacionSuite;
    private Habitacion habitacionNormal;
    private Cliente ana;

    // ===== Datos comunes: Suite con dos visibles (5 y 4) y una oculta (1); Normal con una visible (3) =====
    @BeforeEach
    void setUp() {
        suite = tipoHabitacionRepository.save(TipoHabitacion.builder().nombre("Suite").build());
        normal = tipoHabitacionRepository.save(TipoHabitacion.builder().nombre("Normal").build());
        habitacionSuite = guardarHabitacion("701", suite);
        habitacionNormal = guardarHabitacion("101", normal);
        ana = clienteRepository.save(Cliente.builder()
                .nombre("Ana").apellido("Prueba").email("ana@email.com")
                .password("pass1234").cedula("1001")
                .build());

        guardarResena(guardarReserva(habitacionSuite, "2030-01-01"), suite, 5, true);
        guardarResena(guardarReserva(habitacionSuite, "2030-02-01"), suite, 4, true);
        guardarResena(guardarReserva(habitacionSuite, "2030-03-01"), suite, 1, false);
        guardarResena(guardarReserva(habitacionNormal, "2030-04-01"), normal, 3, true);
    }

    @Test
    void resenaRepository_contarPorCalificacion_excluyeOcultas() {
        // Act
        Map<Integer, Long> conteo = aMapa(resenaRepository.contarPorCalificacion(suite.getId()));

        // Assert: la de 1 estrella está oculta
        assertThat(conteo).containsOnlyKeys(5, 4);
        assertThat(conteo.get(5)).isEqualTo(1L);
        assertThat(conteo.get(4)).isEqualTo(1L);
    }

    @Test
    void resenaRepository_resumenPorTipo_excluyeOcultasYAgrupaPorTipo() {
        // Act
        List<Object[]> filas = resenaRepository.resumenPorTipo();

        // Assert: Suite promedia (5 + 4) / 2 = 4.5 con 2 reseñas; Normal 3.0 con 1
        assertThat(filas).hasSize(2);
        Map<Long, Object[]> porTipo = new HashMap<>();
        filas.forEach(fila -> porTipo.put(((Number) fila[0]).longValue(), fila));

        assertThat(((Number) porTipo.get(suite.getId())[1]).doubleValue()).isEqualTo(4.5);
        assertThat(((Number) porTipo.get(suite.getId())[2]).longValue()).isEqualTo(2L);
        assertThat(((Number) porTipo.get(normal.getId())[1]).doubleValue()).isEqualTo(3.0);
        assertThat(((Number) porTipo.get(normal.getId())[2]).longValue()).isEqualTo(1L);
    }

    @Test
    void resenaRepository_findByTipoHabitacionIdAndVisibleTrue_soloVisiblesMasRecientesPrimero() {
        // Act
        List<Resena> visibles = resenaRepository.findByTipoHabitacionIdAndVisibleTrueOrderByFechaCreacionDesc(suite.getId());

        // Assert: la de febrero es más reciente que la de enero; la de marzo está oculta
        assertThat(visibles).extracting(Resena::getCalificacion).containsExactly(4, 5);
    }

    @Test
    void resenaRepository_findByClienteId_incluyeOcultas() {
        // Act
        List<Resena> deAna = resenaRepository.findByClienteIdOrderByFechaCreacionDesc(ana.getId());

        // Assert
        assertThat(deAna).hasSize(4);
        assertThat(deAna).anyMatch(resena -> !resena.isVisible());
    }

    @Test
    void resenaRepository_buscarParaModeracion_filtrosOpcionales() {
        // Act & Assert
        assertThat(resenaRepository.buscarParaModeracion(null, null)).hasSize(4);
        assertThat(resenaRepository.buscarParaModeracion(suite.getId(), null)).hasSize(3);
        assertThat(resenaRepository.buscarParaModeracion(suite.getId(), 1))
                .singleElement()
                .satisfies(resena -> assertThat(resena.isVisible()).isFalse());
    }

    // ===== RN-3: reserva_id es único en la tabla =====
    @Test
    void resenaRepository_segundaResenaDeLaMismaReserva_violaUnicidad() {
        // Arrange
        Reserva reserva = guardarReserva(habitacionNormal, "2030-05-01");
        guardarResena(reserva, normal, 5, true);

        // Act & Assert
        assertThatThrownBy(() -> resenaRepository.saveAndFlush(construirResena(reserva, normal, 2, true)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ===== Métodos de ayuda =====
    private Map<Integer, Long> aMapa(List<Object[]> filas) {
        Map<Integer, Long> mapa = new HashMap<>();
        filas.forEach(fila -> mapa.put(((Number) fila[0]).intValue(), ((Number) fila[1]).longValue()));
        return mapa;
    }

    private Habitacion guardarHabitacion(String numero, TipoHabitacion tipo) {
        return habitacionRepository.save(Habitacion.builder()
                .numero(numero)
                .tipoHabitacion(tipo)
                .precio(200.0)
                .estado(EstadoHabitacion.DISPONIBLE)
                .build());
    }

    private Reserva guardarReserva(Habitacion habitacion, String inicio) {
        LocalDate fecha = LocalDate.parse(inicio);
        return reservaRepository.save(Reserva.builder()
                .cliente(ana)
                .habitacion(habitacion)
                .fechaInicio(fecha)
                .fechaFin(fecha.plusDays(2))
                .estado(EstadoReserva.FINALIZADA)
                .cantidadPersonas(2)
                .precioNocheAcordado(BigDecimal.valueOf(200))
                .fechaCreacion(LocalDateTime.of(2029, 12, 1, 10, 0))
                .build());
    }

    private void guardarResena(Reserva reserva, TipoHabitacion tipo, int calificacion, boolean visible) {
        resenaRepository.saveAndFlush(construirResena(reserva, tipo, calificacion, visible));
    }

    // La fecha de la reseña es la de salida de la estadía, para que el orden sea predecible
    private Resena construirResena(Reserva reserva, TipoHabitacion tipo, int calificacion, boolean visible) {
        return Resena.builder()
                .reserva(reserva)
                .cliente(ana)
                .tipoHabitacion(tipo)
                .calificacion(calificacion)
                .comentario("A comment long enough to be valid.")
                .visible(visible)
                .fechaCreacion(reserva.getFechaFin().atTime(18, 0))
                .build();
    }
}
