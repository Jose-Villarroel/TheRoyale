package com.theroyale.backend.service;

import com.theroyale.backend.dto.CrearReservaRequest;
import com.theroyale.backend.model.Cliente;
import com.theroyale.backend.model.Cuenta;
import com.theroyale.backend.model.EstadoHabitacion;
import com.theroyale.backend.model.EstadoReserva;
import com.theroyale.backend.model.Habitacion;
import com.theroyale.backend.model.ItemConsumo;
import com.theroyale.backend.model.Reserva;
import com.theroyale.backend.model.TipoHabitacion;
import com.theroyale.backend.repository.HabitacionRepository;
import com.theroyale.backend.repository.ReservaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// ===== Pruebas de ReservaService con mocks =====
// Sin Spring ni base de datos: repositorios y demás servicios se simulan con Mockito, de modo que
// solo se ejecuta la lógica de negocio del servicio.
@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private HabitacionRepository habitacionRepository;

    @Mock
    private CuentaService cuentaService;

    @Mock
    private ClienteService clienteService;

    @Mock
    private TipoHabitacionService tipoHabitacionService;

    @InjectMocks
    private ReservaService reservaService;

    private static final LocalDate INICIO = LocalDate.now().plusDays(10);
    private static final LocalDate FIN = INICIO.plusDays(3);

    // ===== Reservar con disponibilidad =====
    @Test
    void reservaService_crear_conDisponibilidad_guardaPendienteConLaPrimeraHabitacionLibre() {
        // Arrange: hay dos habitaciones libres; el save devuelve lo mismo que recibe
        Habitacion primera = crearHabitacion("101", 200.0);
        Habitacion segunda = crearHabitacion("102", 300.0);
        simularClienteYTipo();
        when(habitacionRepository.buscarDisponibles(eq(1L), eq(INICIO), eq(FIN), anyCollection(), anyCollection(), isNull()))
                .thenReturn(List.of(primera, segunda));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        // Act
        Reserva creada = reservaService.crear(new CrearReservaRequest(1L, 1L, INICIO, FIN, 2));

        // Assert
        assertThat(creada.getEstado()).isEqualTo(EstadoReserva.PENDIENTE);
        assertThat(creada.getHabitacion()).isSameAs(primera);
        assertThat(creada.getPrecioNocheAcordado()).isEqualByComparingTo(BigDecimal.valueOf(200.0));
    }

    // ===== Regla: la reserva se crea si y solo si hay habitaciones disponibles =====
    @Test
    void reservaService_crear_sinDisponibilidad_lanzaExcepcionYNoGuarda() {
        // Arrange: la consulta no encuentra ninguna habitación libre
        simularClienteYTipo();
        when(habitacionRepository.buscarDisponibles(eq(1L), eq(INICIO), eq(FIN), anyCollection(), anyCollection(), isNull()))
                .thenReturn(List.of());

        // Act & Assert
        assertThatThrownBy(() -> reservaService.crear(new CrearReservaRequest(1L, 1L, INICIO, FIN, 2)))
                .isInstanceOf(IllegalStateException.class);
        verify(reservaRepository, never()).save(any());
    }

    // ===== Validación de fechas: falla antes de consultar nada =====
    @Test
    void reservaService_crear_salidaAntesDeLaEntrada_lanzaExcepcionYNoGuarda() {
        // Arrange
        CrearReservaRequest solicitud = new CrearReservaRequest(1L, 1L, FIN, INICIO, 2);

        // Act & Assert
        assertThatThrownBy(() -> reservaService.crear(solicitud))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("check-out");
        verify(reservaRepository, never()).save(any());
    }

    // ===== Regla: acabar la estadía solo se permite con la cuenta pagada =====
    @Test
    void reservaService_hacerCheckOut_conSaldoPendiente_lanzaExcepcionYNoGuarda() {
        // Arrange: estadía en curso cuya cuenta tiene un consumo sin pagar
        Reserva enCurso = Reserva.builder().id(5L).estado(EstadoReserva.EN_CURSO).build();
        Cuenta cuentaConSaldo = Cuenta.builder().build();
        cuentaConSaldo.getItemsConsumo().add(ItemConsumo.builder()
                .cantidad(2)
                .precioUnitario(BigDecimal.valueOf(50))
                .build());
        when(reservaRepository.findById(5L)).thenReturn(Optional.of(enCurso));
        when(cuentaService.buscarPorReserva(5L)).thenReturn(Optional.of(cuentaConSaldo));

        // Act & Assert
        assertThatThrownBy(() -> reservaService.hacerCheckOut(5L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("outstanding balance");
        assertThat(enCurso.getEstado()).isEqualTo(EstadoReserva.EN_CURSO);
        verify(reservaRepository, never()).save(any());
    }

    // ===== Métodos de ayuda =====
    private void simularClienteYTipo() {
        when(clienteService.obtenerPorId(1L)).thenReturn(Cliente.builder().id(1L).build());
        when(tipoHabitacionService.obtenerPorId(1L))
                .thenReturn(TipoHabitacion.builder().id(1L).nombre("Suite").build());
    }

    private Habitacion crearHabitacion(String numero, double precio) {
        return Habitacion.builder()
                .numero(numero)
                .precio(precio)
                .estado(EstadoHabitacion.DISPONIBLE)
                .build();
    }
}
