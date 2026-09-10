package com.theroyale.backend.service;

import com.theroyale.backend.model.Cliente;
import com.theroyale.backend.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class AutenticacionService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Transactional
    public ResultadoRegistro registrarCliente(Cliente cliente, String confirmPassword) {
        if (cliente == null || estaVacio(cliente.getNombre()) || estaVacio(cliente.getApellido()) ||
                estaVacio(cliente.getEmail()) || estaVacio(cliente.getPassword())) {
            return ResultadoRegistro.INCOMPLETO;
        }

        if (!cliente.getPassword().equals(confirmPassword)) {
            return ResultadoRegistro.PASSWORD_NO_COINCIDE;
        }

        String emailNormalizado = normalizarEmail(cliente.getEmail());

        if (emailNormalizado.isBlank() || clienteRepository.findByEmailIgnoreCase(emailNormalizado).isPresent()) {
            return ResultadoRegistro.DUPLICADO;
        }

        cliente.setId(null);
        cliente.setEmail(emailNormalizado);
        cliente.setFechaRegistro(LocalDate.now());
        clienteRepository.save(cliente);
        return ResultadoRegistro.EXITO;
    }

    public Optional<Cliente> autenticar(String email, String password) {
        Optional<Cliente> cliente = clienteRepository.findByEmailIgnoreCase(normalizarEmail(email));

        if (cliente.isEmpty() || password == null || !password.equals(cliente.get().getPassword())) {
            return Optional.empty();
        }

        return cliente;
    }

    public Optional<Cliente> obtenerClienteAutenticado(Long clienteId) {
        if (clienteId == null) {
            return Optional.empty();
        }

        return clienteRepository.findById(clienteId);
    }

    public boolean existeCliente(String email) {
        return clienteRepository.findByEmailIgnoreCase(normalizarEmail(email)).isPresent();
    }

    private boolean estaVacio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }

    private String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    public enum ResultadoRegistro {
        EXITO,
        INCOMPLETO,
        PASSWORD_NO_COINCIDE,
        DUPLICADO
    }
}
