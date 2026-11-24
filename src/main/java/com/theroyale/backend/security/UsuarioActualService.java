package com.theroyale.backend.security;

import com.theroyale.backend.errors.AccesoDenegadoException;
import com.theroyale.backend.model.Cliente;
import com.theroyale.backend.service.ClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

// ===== Datos del usuario autenticado, sacados del SecurityContextHolder (como /details de la guía) =====
// El JwtAuthenticationFilter deja en el contexto el email (username) y los roles del token;
// los controladores lo usan para validar que un cliente solo actúe sobre sus propios datos.
@Service
public class UsuarioActualService {

    @Autowired
    private ClienteService clienteService;

    // Email del usuario autenticado, o null si la petición es anónima
    public String obtenerEmail() {
        Authentication auth = obtenerAutenticacion();
        return auth == null ? null : auth.getName();
    }

    public Set<String> obtenerRoles() {
        Authentication auth = obtenerAutenticacion();
        if (auth == null) {
            return Set.of();
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    public boolean tieneRol(String rol) {
        return obtenerRoles().contains(rol);
    }

    // ===== Cliente sin rol de personal: sus consultas se limitan a sus propios datos =====
    public boolean esSoloCliente() {
        Set<String> roles = obtenerRoles();
        return roles.contains("ROLE_CLIENTE")
                && !roles.contains("ROLE_OPERADOR")
                && !roles.contains("ROLE_ADMIN");
    }

    // ===== Cliente asociado al email del token; 403 si no hay ninguno =====
    public Cliente obtenerClienteActual() {
        String email = obtenerEmail();
        if (email == null) {
            throw AccesoDenegadoException.conCodigo("auth.sesion-cliente-requerida");
        }
        return clienteService.buscarPorEmail(email)
                .orElseThrow(() -> AccesoDenegadoException.conCodigo("auth.sin-cliente-asociado"));
    }

    // ===== Un cliente solo actúa sobre sus propios datos: el clienteId debe ser el de su token =====
    // Al personal (operador o admin) no se le restringe. El clienteId que llega en la URL o en el
    // cuerpo no se confía: se compara con el cliente asociado al email del token (403 si no coincide).
    public void exigirClientePropio(Long clienteId) {
        if (!esSoloCliente()) {
            return;
        }
        if (clienteId == null || !clienteId.equals(obtenerClienteActual().getId())) {
            throw AccesoDenegadoException.conCodigo("auth.datos-ajenos");
        }
    }

    private Authentication obtenerAutenticacion() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return auth;
    }
}
