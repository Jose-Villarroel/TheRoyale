package com.theroyale.backend.service;

import com.theroyale.backend.model.Cliente;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional // cada test revierte sus cambios al terminar, sin afectar los datos de data.sql
class AutenticacionServiceTests {

    @Autowired
    private AutenticacionService autenticacionService;

    @Test
    void autenticaClienteCuandoEmailYPasswordCoinciden() {
        autenticacionService.registrarCliente(new Cliente(null, "Ana", "Lopez", "ANA@EXAMPLE.COM", "clave123", "3001234567", null));

        Optional<Cliente> cliente = autenticacionService.autenticar("ana@example.com", "clave123");

        assertThat(cliente).isPresent();
        assertThat(cliente.get().getEmail()).isEqualTo("ana@example.com");
    }

    @Test
    void rechazaLoginCuandoPasswordNoCoincide() {
        autenticacionService.registrarCliente(new Cliente(null, "Ana", "Lopez", "ana.test@example.com", "clave123", "3001234567", null));

        assertThat(autenticacionService.autenticar("ana.test@example.com", "otra")).isEmpty();
    }

    @Test
    void rechazaRegistroConEmailDuplicado() {
        boolean primero = autenticacionService.registrarCliente(new Cliente(null, "Ana", "Lopez", "ana.dup@example.com", "clave123", "3001234567", null));
        boolean segundo = autenticacionService.registrarCliente(new Cliente(null, "Ana", "Lopez", "ANA.DUP@EXAMPLE.COM", "clave123", "3001234567", null));

        assertThat(primero).isTrue();
        assertThat(segundo).isFalse();
    }
}
