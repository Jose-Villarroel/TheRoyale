package com.theroyale.backend.service;

import com.theroyale.backend.model.Cliente;
import com.theroyale.backend.repository.ClienteRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AutenticacionServiceTests {

    @Test
    void autenticaClienteCuandoEmailYPasswordCoinciden() {
        ClienteRepository clienteRepository = new ClienteRepository();
        AutenticacionService autenticacionService = new AutenticacionService(clienteRepository);

        autenticacionService.registrarCliente(new Cliente(null, "Ana", "Lopez", "ANA@EXAMPLE.COM", "clave123", "3001234567", null));

        Optional<Cliente> cliente = autenticacionService.autenticar("ana@example.com", "clave123");

        assertThat(cliente).isPresent();
        assertThat(cliente.get().getEmail()).isEqualTo("ana@example.com");
    }

    @Test
    void rechazaLoginCuandoPasswordNoCoincide() {
        ClienteRepository clienteRepository = new ClienteRepository();
        AutenticacionService autenticacionService = new AutenticacionService(clienteRepository);

        autenticacionService.registrarCliente(new Cliente(null, "Ana", "Lopez", "ana@example.com", "clave123", "3001234567", null));

        assertThat(autenticacionService.autenticar("ana@example.com", "otra")).isEmpty();
    }

    @Test
    void rechazaRegistroConEmailDuplicado() {
        ClienteRepository clienteRepository = new ClienteRepository();
        AutenticacionService autenticacionService = new AutenticacionService(clienteRepository);

        boolean primero = autenticacionService.registrarCliente(new Cliente(null, "Ana", "Lopez", "ana@example.com", "clave123", "3001234567", null));
        boolean segundo = autenticacionService.registrarCliente(new Cliente(null, "Ana", "Lopez", "ANA@EXAMPLE.COM", "clave123", "3001234567", null));

        assertThat(primero).isTrue();
        assertThat(segundo).isFalse();
    }
}
