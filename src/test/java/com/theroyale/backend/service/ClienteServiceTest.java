package com.theroyale.backend.service;

import com.theroyale.backend.errors.ReglaNegocioException;
import com.theroyale.backend.model.Cliente;
import com.theroyale.backend.model.UserEntity;
import com.theroyale.backend.repository.ClienteRepository;
import com.theroyale.backend.repository.ReservaRepository;
import com.theroyale.backend.repository.RoleRepository;
import com.theroyale.backend.repository.UserEntityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// ===== Pruebas de ClienteService.actualizar: el email del perfil es el username del login =====
// Sin Spring ni base de datos: los repositorios se simulan con Mockito.
@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserEntityRepository userEntityRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ClienteService clienteService;

    @Test
    void actualizar_emailNuevo_actualizaElUsername() {
        // Arrange
        Cliente existente = crearCliente("ana@email.com");
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(clienteRepository.findByEmailIgnoreCase("ana.nueva@email.com")).thenReturn(Optional.empty());
        when(userEntityRepository.existsByUsername("ana.nueva@email.com")).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        // Act: el email llega con mayúsculas y espacios, se normaliza antes de comparar
        Cliente actualizado = clienteService.actualizar(1L, datosNuevos("  Ana.Nueva@Email.com "));

        // Assert
        assertThat(actualizado.getEmail()).isEqualTo("ana.nueva@email.com");
        assertThat(actualizado.getUser().getUsername()).isEqualTo("ana.nueva@email.com");
    }

    @Test
    void actualizar_emailDeOtroUsuario_lanzaErrorYNoGuarda() {
        // Arrange: el email ya es el username de otra cuenta (por ejemplo, un operador)
        Cliente existente = crearCliente("ana@email.com");
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(clienteRepository.findByEmailIgnoreCase("laura@theroyale.com")).thenReturn(Optional.empty());
        when(userEntityRepository.existsByUsername("laura@theroyale.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> clienteService.actualizar(1L, datosNuevos("laura@theroyale.com")))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting(ex -> ((ReglaNegocioException) ex).getCodigo())
                .isEqualTo("cliente.email-en-uso");
        assertThat(existente.getUser().getUsername()).isEqualTo("ana@email.com");
        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    @Test
    void actualizar_mismoEmail_noConsultaUsernames() {
        // Arrange
        Cliente existente = crearCliente("ana@email.com");
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(clienteRepository.findByEmailIgnoreCase("ana@email.com")).thenReturn(Optional.of(existente));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        // Act
        Cliente actualizado = clienteService.actualizar(1L, datosNuevos("ana@email.com"));

        // Assert
        assertThat(actualizado.getUser().getUsername()).isEqualTo("ana@email.com");
        verify(userEntityRepository, never()).existsByUsername(anyString());
    }

    private Cliente crearCliente(String email) {
        UserEntity user = UserEntity.builder().id(10L).username(email).password("hash").build();
        return Cliente.builder()
                .id(1L)
                .nombre("Ana")
                .apellido("Prueba")
                .email(email)
                .cedula("1001")
                .user(user)
                .build();
    }

    private Cliente datosNuevos(String email) {
        return Cliente.builder()
                .nombre("Ana")
                .apellido("Prueba")
                .email(email)
                .telefono("3001234567")
                .build();
    }
}
