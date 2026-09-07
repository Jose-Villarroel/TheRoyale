package com.theroyale.backend.service;

import com.theroyale.backend.model.Cliente;
import com.theroyale.backend.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class AutenticacionService {

    private final ClienteRepository clienteRepository;

    public AutenticacionService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Transactional
    public boolean registrarCliente(Cliente cliente) {
        String emailNormalizado = normalizarEmail(cliente.getEmail());

        if (emailNormalizado.isBlank() || clienteRepository.findByEmailIgnoreCase(emailNormalizado).isPresent()) {
            return false;
        }

        cliente.setId(null);
        cliente.setEmail(emailNormalizado);
        cliente.setFechaRegistro(LocalDate.now());
        clienteRepository.save(cliente);
        return true;
    }

    public Optional<Cliente> autenticar(String email, String password) {
        Optional<Cliente> cliente = clienteRepository.findByEmailIgnoreCase(normalizarEmail(email));

        if (cliente.isEmpty() || password == null || !password.equals(cliente.get().getPassword())) {
            return Optional.empty();
        }

        return cliente;
    }

    public boolean existeCliente(String email) {
        return clienteRepository.findByEmailIgnoreCase(normalizarEmail(email)).isPresent();
    }

    private String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
