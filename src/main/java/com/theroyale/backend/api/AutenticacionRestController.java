package com.theroyale.backend.api;

import com.theroyale.backend.dto.ClienteResponseDto;
import com.theroyale.backend.dto.LoginRequest;
import com.theroyale.backend.dto.LoginResponse;
import com.theroyale.backend.dto.OperadorResponseDto;
import com.theroyale.backend.model.Admin;
import com.theroyale.backend.model.Cliente;
import com.theroyale.backend.model.Operador;
import com.theroyale.backend.repository.AdminRepository;
import com.theroyale.backend.repository.ClienteRepository;
import com.theroyale.backend.repository.OperadorRepository;
import com.theroyale.backend.security.JwtTokenGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

// ===== API REST de autenticación consumida por el frontend y clientes de API =====
@RestController
@RequestMapping("/api/auth")
public class AutenticacionRestController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenGenerator jwtTokenGenerator;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private OperadorRepository operadorRepository;

    @Autowired
    private AdminRepository adminRepository;

    @PostMapping("/login")
    public ResponseEntity<?> iniciarSesion(@RequestBody LoginRequest credenciales) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(credenciales.email().trim(), credenciales.password())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String token = jwtTokenGenerator.generateToken(authentication);

            String email = authentication.getName();

            // Buscar si es cliente, operador o admin
            Optional<Cliente> cliente = clienteRepository.findByEmailIgnoreCase(email);
            if (cliente.isPresent()) {
                Cliente c = cliente.get();
                return ResponseEntity.ok(new LoginResponse("CLIENTE", c.getId(), c.getNombre(), c.getApellido(), c.getEmail(), token));
            }

            Optional<Operador> operador = operadorRepository.findByEmail(email);
            if (operador.isPresent()) {
                Operador o = operador.get();
                return ResponseEntity.ok(new LoginResponse("OPERADOR", o.getId(), o.getNombre(), "", o.getEmail(), token));
            }

            Optional<Admin> admin = adminRepository.findByEmailIgnoreCase(email);
            if (admin.isPresent()) {
                Admin a = admin.get();
                return ResponseEntity.ok(new LoginResponse("ADMIN", a.getId(), a.getNombre(), "", a.getEmail(), token));
            }

            return ResponseEntity.ok(new LoginResponse("USER", 0L, email, "", email, token));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario o contraseña incorrectos");
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> obtenerUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String email = auth.getName();
        Optional<Cliente> cliente = clienteRepository.findByEmailIgnoreCase(email);
        if (cliente.isPresent()) {
            return ResponseEntity.ok(ClienteResponseDto.from(cliente.get()));
        }
        Optional<Operador> operador = operadorRepository.findByEmail(email);
        if (operador.isPresent()) {
            return ResponseEntity.ok(OperadorResponseDto.from(operador.get()));
        }
        return ResponseEntity.ok(auth.getPrincipal());
    }
}
