package com.theroyale.backend.service;

import com.theroyale.backend.errors.RecursoNoEncontradoException;
import com.theroyale.backend.errors.ReglaNegocioException;
import com.theroyale.backend.model.Cliente;
import com.theroyale.backend.model.Role;
import com.theroyale.backend.model.UserEntity;
import com.theroyale.backend.repository.ClienteRepository;
import com.theroyale.backend.repository.ReservaRepository;
import com.theroyale.backend.repository.RoleRepository;
import com.theroyale.backend.repository.UserEntityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// ===== Reglas del CRUD de clientes: email y cédula únicos, contraseña opcional al editar =====
// Un cliente con reservas no se puede borrar, para no dejar reservas sin titular.
@Service
@Transactional(readOnly = true)
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserEntityRepository userEntityRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<Cliente> listarTodos() {
        return clienteRepository.findAll();
    }

    public Optional<Cliente> buscarPorId(Long id) {
        return clienteRepository.findById(id);
    }

    public Cliente obtenerPorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.conCodigo("cliente.no-encontrado", id));
    }

    public Optional<Cliente> buscarPorEmail(String email) {
        return clienteRepository.findByEmailIgnoreCase(normalizarEmail(email));
    }

    @Transactional
    public Cliente crear(Cliente cliente) {
        validarCliente(cliente, true);

        String email = normalizarEmail(cliente.getEmail());
        if (clienteRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw ReglaNegocioException.conflict("cliente.email-duplicado", email);
        }

        String cedula = normalizarCedula(cliente.getCedula());
        if (clienteRepository.findByCedula(cedula).isPresent()) {
            throw ReglaNegocioException.conflict("cliente.cedula-duplicada", cedula);
        }

        cliente.setId(null);
        cliente.setEmail(email);
        cliente.setCedula(cedula);
        cliente.setFechaRegistro(LocalDate.now());

        if (cliente.getUser() == null) {
            Role rol = roleRepository.findByName("ROLE_CLIENTE")
                    .orElseGet(() -> roleRepository.save(new Role("ROLE_CLIENTE")));
            String rawPass = (cliente.getPassword() != null && !cliente.getPassword().isBlank()) ? cliente.getPassword() : "123";
            UserEntity user = UserEntity.builder()
                    .username(email)
                    .password(passwordEncoder.encode(rawPass))
                    .roles(new ArrayList<>(List.of(rol)))
                    .build();
            cliente.setUser(user);
        }

        return clienteRepository.save(cliente);
    }

    @Transactional
    public Cliente actualizar(Long id, Cliente clienteActualizado) {
        Cliente clienteExistente = obtenerPorId(id);

        validarCliente(clienteActualizado, false);

        String email = normalizarEmail(clienteActualizado.getEmail());
        Optional<Cliente> clienteConEmail = clienteRepository.findByEmailIgnoreCase(email);
        if (clienteConEmail.isPresent() && !clienteConEmail.get().getId().equals(id)) {
            throw ReglaNegocioException.conflict("cliente.email-duplicado", email);
        }

        sincronizarUsername(clienteExistente.getUser(), email);

        // La cedula identifica a la persona: no se reescribe desde el formulario de perfil
        clienteExistente.setNombre(clienteActualizado.getNombre());
        clienteExistente.setApellido(clienteActualizado.getApellido());
        clienteExistente.setEmail(email);
        if (!estaVacio(clienteActualizado.getPassword())) {
            clienteExistente.setPassword(clienteActualizado.getPassword());
            if (clienteExistente.getUser() != null) {
                clienteExistente.getUser().setPassword(passwordEncoder.encode(clienteActualizado.getPassword()));
            }
        }
        clienteExistente.setTelefono(clienteActualizado.getTelefono());

        return clienteRepository.save(clienteExistente);
    }

    @Transactional
    public void eliminar(Long id) {
        obtenerPorId(id);
        if (reservaRepository.existsByClienteId(id)) {
            throw ReglaNegocioException.conflict("cliente.tiene-reservas");
        }

        clienteRepository.deleteById(id);
    }

    // ===== El email es el username del login: si cambia, el UserEntity cambia con él =====
    // Sin esto el cliente seguiría entrando con el email viejo y el nuevo no serviría.
    private void sincronizarUsername(UserEntity user, String email) {
        if (user == null || email.equals(user.getUsername())) {
            return;
        }
        if (Boolean.TRUE.equals(userEntityRepository.existsByUsername(email))) {
            throw ReglaNegocioException.conflict("cliente.email-en-uso", email);
        }
        user.setUsername(email);
    }

    private void validarCliente(Cliente cliente, boolean requierePassword) {
        if (cliente == null) {
            throw ReglaNegocioException.badRequest("cliente.requerido");
        }

        if (estaVacio(cliente.getNombre()) || estaVacio(cliente.getApellido()) || estaVacio(cliente.getEmail())) {
            throw ReglaNegocioException.badRequest("cliente.datos-requeridos");
        }

        if (requierePassword) {
            if (estaVacio(cliente.getPassword())) {
                throw ReglaNegocioException.badRequest("cliente.password-requerido");
            }
            if (estaVacio(cliente.getCedula())) {
                throw ReglaNegocioException.badRequest("cliente.cedula-requerida");
            }
        }
    }

    private boolean estaVacio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }

    private String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String normalizarCedula(String cedula) {
        return cedula == null ? "" : cedula.trim();
    }
}
