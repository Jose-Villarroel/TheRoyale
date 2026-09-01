package com.theroyale.backend.service;

import com.theroyale.backend.model.Cliente;
import com.theroyale.backend.repository.ClienteRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public List<Cliente> listarTodos() {
        return clienteRepository.obtenerTodos();
    }

    public Optional<Cliente> buscarPorId(Long id) {
        return clienteRepository.obtenerPorId(id);
    }

    public Optional<Cliente> buscarPorEmail(String email) {
        return clienteRepository.obtenerPorEmail(normalizarEmail(email));
    }

    public Cliente crear(Cliente cliente) {
        validarCliente(cliente, true);

        String email = normalizarEmail(cliente.getEmail());
        if (clienteRepository.obtenerPorEmail(email).isPresent()) {
            throw new IllegalArgumentException("Ya existe un cliente con email: " + email);
        }

        cliente.setId(null);
        cliente.setEmail(email);
        cliente.setFechaRegistro(LocalDate.now());
        return clienteRepository.guardar(cliente);
    }

    public Cliente actualizar(Long id, Cliente clienteActualizado) {
        Cliente clienteExistente = clienteRepository.obtenerPorId(id)
                .orElseThrow(() -> new NoSuchElementException("Cliente no encontrado: " + id));

        validarCliente(clienteActualizado, false);

        String email = normalizarEmail(clienteActualizado.getEmail());
        Optional<Cliente> clienteConEmail = clienteRepository.obtenerPorEmail(email);
        if (clienteConEmail.isPresent() && !clienteConEmail.get().getId().equals(id)) {
            throw new IllegalArgumentException("Ya existe un cliente con email: " + email);
        }

        clienteExistente.setNombre(clienteActualizado.getNombre());
        clienteExistente.setApellido(clienteActualizado.getApellido());
        clienteExistente.setEmail(email);
        if (!estaVacio(clienteActualizado.getPassword())) {
            clienteExistente.setPassword(clienteActualizado.getPassword());
        }
        clienteExistente.setTelefono(clienteActualizado.getTelefono());

        return clienteRepository.guardar(clienteExistente);
    }

    public void eliminar(Long id) {
        if (clienteRepository.obtenerPorId(id).isEmpty()) {
            throw new NoSuchElementException("Cliente no encontrado: " + id);
        }

        clienteRepository.eliminarPorId(id);
    }

    private void validarCliente(Cliente cliente, boolean requierePassword) {
        if (cliente == null) {
            throw new IllegalArgumentException("El cliente es obligatorio.");
        }

        if (estaVacio(cliente.getNombre()) || estaVacio(cliente.getApellido()) || estaVacio(cliente.getEmail())) {
            throw new IllegalArgumentException("Nombre, apellido y email son obligatorios.");
        }

        if (requierePassword && estaVacio(cliente.getPassword())) {
            throw new IllegalArgumentException("La contrasena es obligatoria.");
        }
    }

    private boolean estaVacio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }

    private String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
