package com.theroyale.backend.service;

import com.theroyale.backend.dto.LoginResponse;
import com.theroyale.backend.model.Cliente;
import com.theroyale.backend.repository.AdminRepository;
import com.theroyale.backend.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

// ===== Inicio de sesión de huéspedes, operadores y administradores =====
// Concentra la decisión de "quién es" el usuario detrás del login JSON de AutenticacionRestController.
// Hay un solo login para los tres roles; el tipo devuelto decide a qué panel puede entrar cada uno.
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

    public Optional<Cliente> autenticar(String email, String password) {
        Optional<Cliente> cliente = clienteRepository.findByEmailIgnoreCase(normalizarEmail(email));

        if (cliente.isEmpty() || password == null || !password.equals(cliente.get().getPassword())) {
            return Optional.empty();
        }

        return cliente;
    }

    // ===== Login unificado: primero se busca un huésped y, si no, alguien del personal =====
    // Vacío = credenciales inválidas para ambos.
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

        // Operador y admin no tienen apellido; se manda vacío para no romper la interfaz del frontend.
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

        // Autenticación simple: el email existe y la contraseña es igual, sin token ni hash
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

    private String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
