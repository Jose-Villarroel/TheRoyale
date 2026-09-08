package com.theroyale.backend.service;

import com.theroyale.backend.model.Cliente;
import com.theroyale.backend.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public List<Cliente> listarTodos() {
        return clienteRepository.findAll();
    }

    public Optional<Cliente> buscarPorId(Long id) {
        return clienteRepository.findById(id);
    }

    public Cliente obtenerPorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Cliente no encontrado: " + id));
    }

    public Optional<Cliente> buscarPorEmail(String email) {
        return clienteRepository.findByEmailIgnoreCase(normalizarEmail(email));
    }

    @Transactional
    public Cliente crear(Cliente cliente) {
        validarCliente(cliente, true);

        String email = normalizarEmail(cliente.getEmail());
        if (clienteRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new IllegalArgumentException("Ya existe un cliente con email: " + email);
        }

        cliente.setId(null);
        cliente.setEmail(email);
        cliente.setFechaRegistro(LocalDate.now());
        return clienteRepository.save(cliente);
    }

    @Transactional
    public Cliente actualizar(Long id, Cliente clienteActualizado) {
        Cliente clienteExistente = obtenerPorId(id);

        validarCliente(clienteActualizado, false);

        String email = normalizarEmail(clienteActualizado.getEmail());
        Optional<Cliente> clienteConEmail = clienteRepository.findByEmailIgnoreCase(email);
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

        return clienteRepository.save(clienteExistente);
    }

    @Transactional
    public void eliminar(Long id) {
        obtenerPorId(id);
        clienteRepository.deleteById(id);
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
