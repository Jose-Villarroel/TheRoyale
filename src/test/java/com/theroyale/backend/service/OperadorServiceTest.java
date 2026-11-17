package com.theroyale.backend.service;

import com.theroyale.backend.model.Operador;
import com.theroyale.backend.model.TipoOperador;
import com.theroyale.backend.model.UserEntity;
import com.theroyale.backend.repository.ItemConsumoRepository;
import com.theroyale.backend.repository.OperadorRepository;
import com.theroyale.backend.repository.PagoRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// ===== Pruebas de OperadorService.actualizar: el email del operador es el username del login =====
// Sin Spring ni base de datos: los repositorios se simulan con Mockito.
@ExtendWith(MockitoExtension.class)
class OperadorServiceTest {

    @Mock
    private OperadorRepository operadorRepository;

    @Mock
    private ItemConsumoRepository itemConsumoRepository;

    @Mock
    private PagoRepository pagoRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserEntityRepository userEntityRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private OperadorService operadorService;

    @Test
    void actualizar_emailNuevo_actualizaElUsername() {
        // Arrange
        Operador existente = crearOperador("laura@theroyale.com");
        when(operadorRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(operadorRepository.findByEmail("laura.gomez@theroyale.com")).thenReturn(Optional.empty());
        when(userEntityRepository.existsByUsername("laura.gomez@theroyale.com")).thenReturn(false);
        when(operadorRepository.save(any(Operador.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        // Act
        Operador actualizado = operadorService.actualizar(1L, datosNuevos("Laura.Gomez@TheRoyale.com"));

        // Assert
        assertThat(actualizado.getEmail()).isEqualTo("laura.gomez@theroyale.com");
        assertThat(actualizado.getUser().getUsername()).isEqualTo("laura.gomez@theroyale.com");
    }

    @Test
    void actualizar_emailDeOtroUsuario_lanzaErrorYNoGuarda() {
        // Arrange: el email ya es el username de otra cuenta (por ejemplo, un cliente)
        Operador existente = crearOperador("laura@theroyale.com");
        when(operadorRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(operadorRepository.findByEmail("daniel.castro@email.com")).thenReturn(Optional.empty());
        when(userEntityRepository.existsByUsername("daniel.castro@email.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> operadorService.actualizar(1L, datosNuevos("daniel.castro@email.com")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already used");
        assertThat(existente.getUser().getUsername()).isEqualTo("laura@theroyale.com");
        verify(operadorRepository, never()).save(any(Operador.class));
    }

    private Operador crearOperador(String email) {
        UserEntity user = UserEntity.builder().id(20L).username(email).password("hash").build();
        return Operador.builder()
                .id(1L)
                .nombre("Laura Gomez")
                .email(email)
                .tipo(TipoOperador.OPERADOR)
                .user(user)
                .build();
    }

    private Operador datosNuevos(String email) {
        return Operador.builder()
                .nombre("Laura Gomez")
                .email(email)
                .build();
    }
}
