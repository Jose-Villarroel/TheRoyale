package com.theroyale.backend.errors;

import org.springframework.security.core.userdetails.UsernameNotFoundException;

// ===== No existe ninguna cuenta con ese username (el email, en esta aplicación) =====
// Hereda de UsernameNotFoundException porque Spring Security la espera desde UserDetailsService:
// así el AuthenticationManager sigue tratándola como un fallo de autenticación. Lo que añade es el
// username, para que ApiExceptionHandler pueda resolver auth.usuario-no-encontrado con su parámetro.
//
// En POST /api/auth/login NO llega al manejador: el controlador atrapa AuthenticationException y
// responde 401 auth.credenciales-invalidas, para no revelar si el correo está registrado o no.
public class UsuarioNoEncontradoException extends UsernameNotFoundException {

    private final String username;

    public UsuarioNoEncontradoException(String username) {
        super("No account found for username: " + username);
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
