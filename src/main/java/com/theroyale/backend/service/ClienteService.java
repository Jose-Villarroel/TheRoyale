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
    private final AutenticacionService autenticacionService;

    public ClienteService(ClienteRepository clienteRepository, AutenticacionService autenticacionService) {
        this.clienteRepository = clienteRepository;
        this.autenticacionService = autenticacionService;
    }

    public List<Cliente> listarTodos() {
        return clienteRepository.obtenerTodos();
    }

    public Optional<Cliente> buscarPorId(Long id) {
        return clienteRepository.obtenerPorId(id);
    }

    public Optional<Cliente> buscarPorEmailUsuario(String emailUsuario) {
        return clienteRepository.obtenerPorEmailUsuario(normalizarEmail(emailUsuario));
    }

    public Cliente crear(Cliente cliente) {
        validarCliente(cliente);

        String emailUsuario = normalizarEmail(cliente.getEmailUsuario());
        if (clienteRepository.obtenerPorEmailUsuario(emailUsuario).isPresent()) {
            throw new IllegalArgumentException("Ya existe un cliente vinculado al usuario: " + emailUsuario);
        }

        cliente.setId(null);
        cliente.setEmailUsuario(emailUsuario);
        cliente.setFechaRegistro(LocalDate.now());
        return clienteRepository.guardar(cliente);
    }

    public Cliente actualizar(Long id, Cliente clienteActualizado) {
        Cliente clienteExistente = clienteRepository.obtenerPorId(id)
                .orElseThrow(() -> new NoSuchElementException("Cliente no encontrado: " + id));

        validarCliente(clienteActualizado);

        String emailUsuario = normalizarEmail(clienteActualizado.getEmailUsuario());
        Optional<Cliente> clienteConEmail = clienteRepository.obtenerPorEmailUsuario(emailUsuario);
        if (clienteConEmail.isPresent() && !clienteConEmail.get().getId().equals(id)) {
            throw new IllegalArgumentException("Ya existe un cliente vinculado al usuario: " + emailUsuario);
        }

        clienteExistente.setNombre(clienteActualizado.getNombre());
        clienteExistente.setApellido(clienteActualizado.getApellido());
        clienteExistente.setEmailUsuario(emailUsuario);
        clienteExistente.setTelefono(clienteActualizado.getTelefono());

        return clienteRepository.guardar(clienteExistente);
    }

    public void eliminar(Long id) {
        if (clienteRepository.obtenerPorId(id).isEmpty()) {
            throw new NoSuchElementException("Cliente no encontrado: " + id);
        }

        clienteRepository.eliminarPorId(id);
    }

    private void validarCliente(Cliente cliente) {
        if (cliente == null) {
            throw new IllegalArgumentException("El cliente es obligatorio.");
        }

        if (estaVacio(cliente.getNombre()) || estaVacio(cliente.getApellido()) || estaVacio(cliente.getEmailUsuario())) {
            throw new IllegalArgumentException("Nombre, apellido y email de usuario son obligatorios.");
        }

        String emailUsuario = normalizarEmail(cliente.getEmailUsuario());
        if (!autenticacionService.existeUsuario(emailUsuario)) {
            throw new IllegalArgumentException("No existe un usuario autenticable con email: " + emailUsuario);
        }
    }

    private boolean estaVacio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }

    private String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
