package com.theroyale.backend.repository;

import com.theroyale.backend.model.Cliente;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class ClienteRepository {

    private final List<Cliente> clientes = new ArrayList<>();
    private Long siguienteId = 1L;

    public List<Cliente> obtenerTodos() {
        return clientes;
    }

    public Optional<Cliente> obtenerPorId(Long id) {
        return clientes.stream()
                .filter(cliente -> cliente.getId().equals(id))
                .findFirst();
    }

    public Optional<Cliente> obtenerPorEmail(String email) {
        return clientes.stream()
                .filter(cliente -> cliente.getEmail() != null && cliente.getEmail().equalsIgnoreCase(email))
                .findFirst();
    }

    public Cliente guardar(Cliente cliente) {
        if (cliente.getId() == null) {
            cliente.setId(siguienteId);
            siguienteId++;
            clientes.add(cliente);
        } else {
            eliminarPorId(cliente.getId());
            clientes.add(cliente);
        }
        return cliente;
    }

    public void eliminarPorId(Long id) {
        clientes.removeIf(cliente -> cliente.getId().equals(id));
    }
}
