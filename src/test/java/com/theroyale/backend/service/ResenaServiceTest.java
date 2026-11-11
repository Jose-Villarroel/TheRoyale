package com.theroyale.backend.service;

import com.theroyale.backend.dto.CrearResenaRequest;
import com.theroyale.backend.dto.ResumenResenasResponse;
import com.theroyale.backend.errors.AccesoDenegadoException;
import com.theroyale.backend.errors.RecursoNoEncontradoException;
import com.theroyale.backend.model.Cliente;
import com.theroyale.backend.model.EstadoHabitacion;
import com.theroyale.backend.model.EstadoReserva;
import com.theroyale.backend.model.Habitacion;
import com.theroyale.backend.model.Resena;
import com.theroyale.backend.model.Reserva;
import com.theroyale.backend.model.TipoHabitacion;
import com.theroyale.backend.repository.ResenaRepository;
import com.theroyale.backend.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// ===== Pruebas de ResenaService con mocks (reglas RN-1 a RN-10) =====
// Sin Spring ni base de datos: repositorios y servicios se simulan con Mockito. Que las consultas
// excluyan las reseñas ocultas se prueba contra H2 en ResenaRepositoryTest.
@ExtendWith(MockitoExtension.class)
class ResenaServiceTest {

    private static final String COMENTARIO_VALIDO = "Spotless room and very attentive staff.";

    @Mock
    private ResenaRepository resenaRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private ClienteService clienteService;

    @Mock
    private TipoHabitacionService tipoHabitacionService;

    @InjectMocks
    private ResenaService resenaService;

    private TipoHabitacion luxury;
    private Cliente laura;
    private Cliente otroCliente;

    @BeforeEach
    void setUp() {
        luxury = TipoHabitacion.builder().id(4L).nombre("Luxury").build();
        laura = Cliente.builder().id(8L).nombre("Laura").apellido("Martínez").build();
        otroCliente = Cliente.builder().id(9L).nombre("Pedro").apellido("Ruiz").build();
    }

    // ===== Crear: caso feliz =====
    @Test
    void resenaService_crear_reservaFinalizadaPropia_guardaVisibleConElTipoDeLaReserva() {
        // Arrange
        Reserva reserva = crearReserva(12L, laura, EstadoReserva.FINALIZADA);
        when(reservaRepository.findById(12L)).thenReturn(Optional.of(reserva));
        when(resenaRepository.existsByReservaId(12L)).thenReturn(false);
        when(resenaRepository.save(any(Resena.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        // Act
        Resena creada = resenaService.crear(new CrearResenaRequest(12L, 8L, 5,
                "  Perfect city view  ", "  " + COMENTARIO_VALIDO + "  "));

        // Assert: RN-6 el tipo sale de la habitación de la reserva; título y comentario se recortan
        assertThat(creada.getTipoHabitacion()).isSameAs(luxury);
        assertThat(creada.getCliente()).isSameAs(laura);
        assertThat(creada.getReserva()).isSameAs(reserva);
        assertThat(creada.getCalificacion()).isEqualTo(5);
        assertThat(creada.getTitulo()).isEqualTo("Perfect city view");
        assertThat(creada.getComentario()).isEqualTo(COMENTARIO_VALIDO);
        assertThat(creada.isVisible()).isTrue();
        assertThat(creada.getFechaCreacion()).isNotNull();
        assertThat(creada.getFechaModificacion()).isNull();
        assertThat(reserva.getResena()).isSameAs(creada);
    }

    // ===== RN-1: solo estadías FINALIZADA =====
    @ParameterizedTest
    @EnumSource(value = EstadoReserva.class, names = {"PENDIENTE", "CONFIRMADA", "EN_CURSO", "CANCELADA"})
    void resenaService_crear_reservaNoFinalizada_lanzaConflictoYNoGuarda(EstadoReserva estado) {
        // Arrange
        when(reservaRepository.findById(12L)).thenReturn(Optional.of(crearReserva(12L, laura, estado)));

        // Act & Assert
        assertThatThrownBy(() -> resenaService.crear(solicitudValida(12L, 8L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only completed stays can be reviewed.");
        verify(resenaRepository, never()).save(any());
    }

    // ===== RN-3: una sola reseña por reserva =====
    @Test
    void resenaService_crear_reservaYaResenada_lanzaConflictoYNoGuarda() {
        // Arrange
        when(reservaRepository.findById(12L)).thenReturn(Optional.of(crearReserva(12L, laura, EstadoReserva.FINALIZADA)));
        when(resenaRepository.existsByReservaId(12L)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> resenaService.crear(solicitudValida(12L, 8L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("This reservation already has a review.");
        verify(resenaRepository, never()).save(any());
    }

    // ===== RN-2: la reserva debe ser del cliente =====
    @Test
    void resenaService_crear_reservaAjena_lanzaAccesoDenegadoYNoGuarda() {
        // Arrange: la reserva es de Laura (8) y la reseña la intenta el cliente 9
        when(reservaRepository.findById(12L)).thenReturn(Optional.of(crearReserva(12L, laura, EstadoReserva.FINALIZADA)));

        // Act & Assert
        assertThatThrownBy(() -> resenaService.crear(solicitudValida(12L, 9L)))
                .isInstanceOf(AccesoDenegadoException.class)
                .hasMessage("This reservation does not belong to you.");
        verify(resenaRepository, never()).save(any());
    }

    @Test
    void resenaService_crear_reservaInexistente_lanzaNoEncontrado() {
        // Arrange
        when(reservaRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> resenaService.crear(solicitudValida(99L, 8L)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    // ===== RN-4: calificación entera entre 1 y 5; falla antes de consultar nada =====
    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, 6, -1})
    void resenaService_crear_calificacionFueraDeRango_lanzaArgumentoInvalido(Integer calificacion) {
        // Arrange
        CrearResenaRequest solicitud = new CrearResenaRequest(12L, 8L, calificacion, null, COMENTARIO_VALIDO);

        // Act & Assert
        assertThatThrownBy(() -> resenaService.crear(solicitud))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rating");
        verifyNoInteractions(reservaRepository, resenaRepository);
    }

    // ===== RN-5: comentario de 10 a 1000 caracteres tras el trim =====
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   corto   ", "123456789"})
    void resenaService_crear_comentarioDemasiadoCorto_lanzaArgumentoInvalido(String comentario) {
        // Arrange
        CrearResenaRequest solicitud = new CrearResenaRequest(12L, 8L, 4, null, comentario);

        // Act & Assert
        assertThatThrownBy(() -> resenaService.crear(solicitud))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("comment");
        verifyNoInteractions(reservaRepository, resenaRepository);
    }

    @Test
    void resenaService_crear_comentarioDeMasDe1000Caracteres_lanzaArgumentoInvalido() {
        // Arrange
        CrearResenaRequest solicitud = new CrearResenaRequest(12L, 8L, 4, null, "a".repeat(1001));

        // Act & Assert
        assertThatThrownBy(() -> resenaService.crear(solicitud))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("comment");
        verifyNoInteractions(reservaRepository, resenaRepository);
    }

    @Test
    void resenaService_crear_comentarioDe1000YTituloDe100_esValido() {
        // Arrange: los límites exactos se aceptan
        when(reservaRepository.findById(12L)).thenReturn(Optional.of(crearReserva(12L, laura, EstadoReserva.FINALIZADA)));
        when(resenaRepository.save(any(Resena.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        // Act
        Resena creada = resenaService.crear(new CrearResenaRequest(12L, 8L, 3, "t".repeat(100), "a".repeat(1000)));

        // Assert
        assertThat(creada.getComentario()).hasSize(1000);
        assertThat(creada.getTitulo()).hasSize(100);
    }

    @Test
    void resenaService_crear_tituloDeMasDe100Caracteres_lanzaArgumentoInvalido() {
        // Arrange
        CrearResenaRequest solicitud = new CrearResenaRequest(12L, 8L, 4, "t".repeat(101), COMENTARIO_VALIDO);

        // Act & Assert
        assertThatThrownBy(() -> resenaService.crear(solicitud))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title");
        verifyNoInteractions(reservaRepository, resenaRepository);
    }

    // ===== RN-7: editar =====
    @Test
    void resenaService_actualizar_porElAutor_cambiaContenidoYMarcaFechaModificacion() {
        // Arrange
        Reserva reserva = crearReserva(12L, laura, EstadoReserva.FINALIZADA);
        Resena resena = crearResena(3L, reserva, 5, true);
        when(resenaRepository.findById(3L)).thenReturn(Optional.of(resena));
        when(resenaRepository.save(any(Resena.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        // Act: reservaId del cuerpo se ignora
        Resena editada = resenaService.actualizar(3L,
                new CrearResenaRequest(777L, 8L, 3, "", "The room was fine, nothing special."));

        // Assert
        assertThat(editada.getCalificacion()).isEqualTo(3);
        assertThat(editada.getTitulo()).isNull();
        assertThat(editada.getComentario()).isEqualTo("The room was fine, nothing special.");
        assertThat(editada.getFechaModificacion()).isNotNull();
        assertThat(editada.getReserva()).isSameAs(reserva);
        assertThat(editada.getTipoHabitacion()).isSameAs(luxury);
    }

    @Test
    void resenaService_actualizar_porOtroCliente_lanzaAccesoDenegadoYNoGuarda() {
        // Arrange
        Resena resena = crearResena(3L, crearReserva(12L, laura, EstadoReserva.FINALIZADA), 5, true);
        when(resenaRepository.findById(3L)).thenReturn(Optional.of(resena));

        // Act & Assert
        assertThatThrownBy(() -> resenaService.actualizar(3L, solicitudValida(null, otroCliente.getId())))
                .isInstanceOf(AccesoDenegadoException.class);
        assertThat(resena.getCalificacion()).isEqualTo(5);
        verify(resenaRepository, never()).save(any());
    }

    // ===== RN-7: eliminar =====
    @Test
    void resenaService_eliminar_porElAutor_borraLaResena() {
        // Arrange
        Reserva reserva = crearReserva(12L, laura, EstadoReserva.FINALIZADA);
        Resena resena = crearResena(3L, reserva, 5, true);
        reserva.setResena(resena);
        when(resenaRepository.findById(3L)).thenReturn(Optional.of(resena));

        // Act
        resenaService.eliminar(3L, 8L);

        // Assert
        verify(resenaRepository).delete(resena);
        assertThat(reserva.getResena()).isNull();
    }

    @Test
    void resenaService_eliminar_porOtroCliente_lanzaAccesoDenegadoYNoBorra() {
        // Arrange
        Resena resena = crearResena(3L, crearReserva(12L, laura, EstadoReserva.FINALIZADA), 5, true);
        when(resenaRepository.findById(3L)).thenReturn(Optional.of(resena));

        // Act & Assert
        assertThatThrownBy(() -> resenaService.eliminar(3L, otroCliente.getId()))
                .isInstanceOf(AccesoDenegadoException.class);
        verify(resenaRepository, never()).delete(any());
    }

    // ===== RN-8: el admin oculta y vuelve a mostrar =====
    @Test
    void resenaService_ocultarYMostrar_cambiaLaVisibilidad() {
        // Arrange
        Resena resena = crearResena(3L, crearReserva(12L, laura, EstadoReserva.FINALIZADA), 5, true);
        when(resenaRepository.findById(3L)).thenReturn(Optional.of(resena));
        when(resenaRepository.save(any(Resena.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        // Act & Assert
        assertThat(resenaService.ocultar(3L).isVisible()).isFalse();
        assertThat(resenaService.mostrar(3L).isVisible()).isTrue();
    }

    // ===== RN-8 y RN-10: resumen con reseñas visibles =====
    @Test
    void resenaService_resumen_conResenas_calculaPromedioRedondeadoYDistribucionCompleta() {
        // Arrange: la consulta ya excluye las ocultas; devuelve dos de 5 estrellas y una de 4
        when(tipoHabitacionService.obtenerPorId(4L)).thenReturn(luxury);
        when(resenaRepository.contarPorCalificacion(4L)).thenReturn(List.of(
                new Object[]{5, 2L},
                new Object[]{4, 1L}));

        // Act
        ResumenResenasResponse resumen = resenaService.resumen(4L);

        // Assert: 14 / 3 = 4.666... -> 4.7
        assertThat(resumen.tipoHabitacionId()).isEqualTo(4L);
        assertThat(resumen.promedio()).isEqualTo(4.7);
        assertThat(resumen.total()).isEqualTo(3);
        assertThat(resumen.distribucion()).containsExactly(
                entry(1, 0L), entry(2, 0L), entry(3, 0L), entry(4, 1L), entry(5, 2L));
    }

    // ===== RN-10: sin reseñas visibles (p. ej., todas ocultas) =====
    @Test
    void resenaService_resumen_sinResenasVisibles_devuelvePromedioNuloYTotalCero() {
        // Arrange
        when(tipoHabitacionService.obtenerPorId(4L)).thenReturn(luxury);
        when(resenaRepository.contarPorCalificacion(4L)).thenReturn(List.of());

        // Act
        ResumenResenasResponse resumen = resenaService.resumen(4L);

        // Assert
        assertThat(resumen.promedio()).isNull();
        assertThat(resumen.total()).isZero();
        assertThat(resumen.distribucion()).containsOnlyKeys(1, 2, 3, 4, 5);
        assertThat(resumen.distribucion().values()).containsOnly(0L);
    }

    @Test
    void resenaService_resumenTodos_incluyeTiposSinResenasConPromedioNulo() {
        // Arrange: Luxury tiene reseñas (promedio 4.25 -> 4.3), Normal ninguna
        TipoHabitacion normal = TipoHabitacion.builder().id(1L).nombre("Normal").build();
        when(tipoHabitacionService.listarTodos()).thenReturn(List.of(normal, luxury));
        when(resenaRepository.resumenPorTipo()).thenReturn(List.<Object[]>of(new Object[]{4L, 4.25, 4L}));
        when(resenaRepository.contarPorTipoYCalificacion()).thenReturn(List.of(
                new Object[]{4L, 5, 2L},
                new Object[]{4L, 4, 1L},
                new Object[]{4L, 3, 1L}));

        // Act
        List<ResumenResenasResponse> resumenes = resenaService.resumenTodos();

        // Assert
        assertThat(resumenes).hasSize(2);
        ResumenResenasResponse deNormal = resumenes.get(0);
        assertThat(deNormal.tipoHabitacionId()).isEqualTo(1L);
        assertThat(deNormal.promedio()).isNull();
        assertThat(deNormal.total()).isZero();
        assertThat(deNormal.distribucion()).containsOnlyKeys(1, 2, 3, 4, 5);

        ResumenResenasResponse deLuxury = resumenes.get(1);
        assertThat(deLuxury.promedio()).isEqualTo(4.3);
        assertThat(deLuxury.total()).isEqualTo(4);
        assertThat(deLuxury.distribucion()).containsExactly(
                entry(1, 0L), entry(2, 0L), entry(3, 1L), entry(4, 1L), entry(5, 2L));
    }

    @Test
    void resenaService_listarParaModeracion_calificacionInvalida_lanzaArgumentoInvalido() {
        // Act & Assert
        assertThatThrownBy(() -> resenaService.listarParaModeracion(null, 7))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(resenaRepository);
    }

    // ===== RN-9: la usa ReservaService.eliminar =====
    @Test
    void resenaService_eliminarPorReserva_conResena_laBorra() {
        // Arrange
        Reserva reserva = crearReserva(12L, laura, EstadoReserva.FINALIZADA);
        Resena resena = crearResena(3L, reserva, 4, true);
        when(resenaRepository.findByReservaId(12L)).thenReturn(Optional.of(resena));

        // Act
        resenaService.eliminarPorReserva(reserva);

        // Assert
        verify(resenaRepository).delete(resena);
    }

    @Test
    void resenaService_eliminarPorReserva_sinResena_noBorraNada() {
        // Arrange
        Reserva reserva = crearReserva(12L, laura, EstadoReserva.FINALIZADA);
        when(resenaRepository.findByReservaId(12L)).thenReturn(Optional.empty());

        // Act
        resenaService.eliminarPorReserva(reserva);

        // Assert
        verify(resenaRepository, never()).delete(any());
    }

    // ===== Métodos de ayuda =====
    private CrearResenaRequest solicitudValida(Long reservaId, Long clienteId) {
        return new CrearResenaRequest(reservaId, clienteId, 5, "Perfect city view", COMENTARIO_VALIDO);
    }

    private Reserva crearReserva(Long id, Cliente cliente, EstadoReserva estado) {
        Habitacion habitacion = Habitacion.builder()
                .id(70L)
                .numero("704")
                .tipoHabitacion(luxury)
                .precio(370.0)
                .estado(EstadoHabitacion.DISPONIBLE)
                .build();
        return Reserva.builder()
                .id(id)
                .cliente(cliente)
                .habitacion(habitacion)
                .fechaInicio(LocalDate.of(2026, 11, 3))
                .fechaFin(LocalDate.of(2026, 11, 6))
                .estado(estado)
                .cantidadPersonas(2)
                .build();
    }

    private Resena crearResena(Long id, Reserva reserva, int calificacion, boolean visible) {
        return Resena.builder()
                .id(id)
                .reserva(reserva)
                .cliente(reserva.getCliente())
                .tipoHabitacion(luxury)
                .calificacion(calificacion)
                .titulo("Perfect city view")
                .comentario(COMENTARIO_VALIDO)
                .visible(visible)
                .fechaCreacion(LocalDateTime.of(2026, 11, 6, 18, 30))
                .build();
    }
}
