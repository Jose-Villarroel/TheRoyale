package com.theroyale.backend.service;

import com.theroyale.backend.dto.LoginResponse;
import com.theroyale.backend.model.Cliente;
import com.theroyale.backend.repository.AdminRepository;
import com.theroyale.backend.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

// ===== Registro e inicio de sesión de huéspedes, operadores y administradores =====
@Service
@Transactional(readOnly = true)
public class AutenticacionService {

    public static final String TIPO_CLIENTE = "CLIENTE";
    public static final String TIPO_OPERADOR = "OPERADOR";
    public static final String TIPO_ADMIN = "ADMIN";

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private OperadorService operadorService;

    @Transactional
    public ResultadoRegistro registrarCliente(Cliente cliente, String confirmPassword) {
        if (cliente == null || estaVacio(cliente.getNombre()) || estaVacio(cliente.getApellido()) ||
                estaVacio(cliente.getEmail()) || estaVacio(cliente.getPassword()) ||
                estaVacio(cliente.getCedula())) {
            return ResultadoRegistro.INCOMPLETO;
        }

        if (!cliente.getPassword().equals(confirmPassword)) {
            return ResultadoRegistro.PASSWORD_NO_COINCIDE;
        }

        String emailNormalizado = normalizarEmail(cliente.getEmail());
        String cedulaNormalizada = cliente.getCedula().trim();

        if (emailNormalizado.isBlank() || clienteRepository.findByEmailIgnoreCase(emailNormalizado).isPresent()
                || clienteRepository.findByCedula(cedulaNormalizada).isPresent()) {
            return ResultadoRegistro.DUPLICADO;
        }

        cliente.setId(null);
        cliente.setEmail(emailNormalizado);
        cliente.setCedula(cedulaNormalizada);
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

    // ===== Login unificado: primero se busca un huésped y, si no, alguien del personal =====
    public Optional<LoginResponse> login(String email, String password) {
        Optional<Cliente> cliente = autenticar(email, password);
        if (cliente.isPresent()) {
            Cliente autenticado = cliente.get();
            return Optional.of(new LoginResponse(
                    TIPO_CLIENTE,
                    autenticado.getId(),
                    autenticado.getNombre(),
                    autenticado.getApellido(),
                    autenticado.getEmail()));
        }

        Optional<LoginResponse> operador = operadorService.autenticar(email, password)
                .map(autenticado -> new LoginResponse(
                        TIPO_OPERADOR,
                        autenticado.getId(),
                        autenticado.getNombre(),
                        "",
                        autenticado.getEmail()));
        if (operador.isPresent()) {
            return operador;
        }

        if (email == null || password == null) {
            return Optional.empty();
        }
        return adminRepository.findByEmailIgnoreCase(email.trim())
                .filter(admin -> password.equals(admin.getPassword()))
                .map(admin -> new LoginResponse(
                        TIPO_ADMIN,
                        admin.getId(),
                        admin.getNombre(),
                        "",
                        admin.getEmail()));
    }

    @Transactional
    public LoginResponse loginConGoogle(String email, String nombre, String apellido) {
        String emailNorm = normalizarEmail(email);
        Cliente cliente = clienteRepository.findByEmailIgnoreCase(emailNorm)
                .orElseGet(() -> {
                    String cedulaGen = "GGL" + System.currentTimeMillis();
                    Cliente nuevo = Cliente.builder()
                            .nombre(nombre != null && !nombre.isBlank() ? nombre.trim() : "Google")
                            .apellido(apellido != null && !apellido.isBlank() ? apellido.trim() : "Guest")
                            .email(emailNorm)
                            .cedula(cedulaGen)
                            .password("GoogleOAuth2SecurePass")
                            .telefono("+1-555-0000")
                            .fechaRegistro(LocalDate.now())
                            .build();
                    return clienteRepository.save(nuevo);
                });

        return new LoginResponse(
                TIPO_CLIENTE,
                cliente.getId(),
                cliente.getNombre(),
                cliente.getApellido(),
                cliente.getEmail()
        );
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
