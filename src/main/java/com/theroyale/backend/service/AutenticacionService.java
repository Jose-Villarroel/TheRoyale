package com.theroyale.backend.service;

import com.theroyale.backend.model.Usuario;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AutenticacionService {

    private final Map<String, Usuario> usuarios = new ConcurrentHashMap<>();

    public AutenticacionService() {
        registrarUsuario(new Usuario("Guest", "Royale", "you@example.com", "password"));
    }

    public boolean registrarUsuario(Usuario usuario) {
        String emailNormalizado = normalizarEmail(usuario.getEmail());

        if (emailNormalizado.isBlank() || usuarios.containsKey(emailNormalizado)) {
            return false;
        }

        usuario.setEmail(emailNormalizado);
        usuarios.put(emailNormalizado, usuario);
        return true;
    }

    public Optional<Usuario> autenticar(String email, String password) {
        Usuario usuario = usuarios.get(normalizarEmail(email));

        if (usuario == null || password == null || !usuario.getPassword().equals(password)) {
            return Optional.empty();
        }

        return Optional.of(usuario);
    }

    public boolean existeUsuario(String email) {
        return usuarios.containsKey(normalizarEmail(email));
    }

    private String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
