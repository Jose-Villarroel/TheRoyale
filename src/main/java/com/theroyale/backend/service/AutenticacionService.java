package com.theroyale.backend.service;

import com.theroyale.backend.model.Cliente;
import com.theroyale.backend.repository.ClienteRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class AutenticacionService {

    private final ClienteRepository clienteRepository;

    public AutenticacionService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
        registrarCliente(new Cliente(null, "Guest", "Royale", "you@example.com", "password", "", null));
    }

    public boolean registrarCliente(Cliente cliente) {
        String emailNormalizado = normalizarEmail(cliente.getEmail());

        if (emailNormalizado.isBlank() || clienteRepository.obtenerPorEmail(emailNormalizado).isPresent()) {
            return false;
        }

        cliente.setId(null);
        cliente.setEmail(emailNormalizado);
        cliente.setFechaRegistro(LocalDate.now());
        clienteRepository.guardar(cliente);
        return true;
    }

    public Optional<Cliente> autenticar(String email, String password) {
        Optional<Cliente> cliente = clienteRepository.obtenerPorEmail(normalizarEmail(email));

        if (cliente.isEmpty() || password == null || !password.equals(cliente.get().getPassword())) {
            return Optional.empty();
        }

        return cliente;
    }

    public boolean existeCliente(String email) {
        return clienteRepository.obtenerPorEmail(normalizarEmail(email)).isPresent();
    }

    private String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
