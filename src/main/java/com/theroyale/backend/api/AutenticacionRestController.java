package com.theroyale.backend.api;

import com.theroyale.backend.dto.ClienteResponseDto;
import com.theroyale.backend.dto.GoogleLoginRequest;
import com.theroyale.backend.dto.GoogleUserInfo;
import com.theroyale.backend.dto.LoginRequest;
import com.theroyale.backend.dto.LoginResponse;
import com.theroyale.backend.dto.OperadorResponseDto;
import com.theroyale.backend.model.Admin;
import com.theroyale.backend.model.Cliente;
import com.theroyale.backend.model.Operador;
import com.theroyale.backend.model.Role;
import com.theroyale.backend.model.UserEntity;
import com.theroyale.backend.repository.AdminRepository;
import com.theroyale.backend.repository.ClienteRepository;
import com.theroyale.backend.repository.OperadorRepository;
import com.theroyale.backend.repository.RoleRepository;
import com.theroyale.backend.repository.UserEntityRepository;
import com.theroyale.backend.security.JwtTokenGenerator;
import com.theroyale.backend.service.GoogleAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    @Autowired
    private GoogleAuthService googleAuthService;

    @Autowired
    private UserEntityRepository userEntityRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

    @PostMapping("/google")
    public ResponseEntity<?> iniciarSesionConGoogle(@RequestBody GoogleLoginRequest request) {
        if (request == null || request.idToken() == null || request.idToken().isBlank()) {
            return ResponseEntity.badRequest().body("El token de Google es requerido");
        }

        Optional<GoogleUserInfo> userInfoOpt = googleAuthService.verificarToken(request.idToken());
        if (userInfoOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token de Google inválido o expirado");
        }

        GoogleUserInfo userInfo = userInfoOpt.get();
        String email = userInfo.email().toLowerCase().trim();

        // 1. RESTRICCIÓN: Exclusivo para clientes / huéspedes
        if (adminRepository.findByEmailIgnoreCase(email).isPresent() || operadorRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("El inicio de sesión con Google está habilitado únicamente para huéspedes (clientes). Administradores y operadores deben ingresar con sus credenciales.");
        }

        // 2. Buscar o auto-registrar Cliente
        Role rolCliente = roleRepository.findByName("ROLE_CLIENTE")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_CLIENTE")));

        Cliente cliente = clienteRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            UserEntity userEntity = userEntityRepository.findByUsername(email).orElseGet(() ->
                    userEntityRepository.save(UserEntity.builder()
                            .username(email)
                            .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                            .roles(new ArrayList<>(List.of(rolCliente)))
                            .build())
            );

            String sub = userInfo.sub();
            String cedulaTemp = "G-" + (sub.length() > 10 ? sub.substring(sub.length() - 10) : sub);
            String nombre = (userInfo.givenName() != null && !userInfo.givenName().isBlank())
                    ? userInfo.givenName()
                    : userInfo.name();
            String apellido = (userInfo.familyName() != null && !userInfo.familyName().isBlank())
                    ? userInfo.familyName()
                    : "";

            Cliente nuevo = Cliente.builder()
                    .nombre(nombre)
                    .apellido(apellido)
                    .email(email)
                    .cedula(cedulaTemp)
                    .password(userEntity.getPassword())
                    .fechaRegistro(LocalDate.now())
                    .user(userEntity)
                    .build();

            return clienteRepository.save(nuevo);
        });

        if (cliente.getUser() == null) {
            UserEntity userEntity = userEntityRepository.findByUsername(email).orElseGet(() ->
                    userEntityRepository.save(UserEntity.builder()
                            .username(email)
                            .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                            .roles(new ArrayList<>(List.of(rolCliente)))
                            .build())
            );
            cliente.setUser(userEntity);
            clienteRepository.save(cliente);
        }

        // 3. Crear autenticación y generar JWT de la aplicación
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_CLIENTE"));
        Authentication authentication = new UsernamePasswordAuthenticationToken(email, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String appJwt = jwtTokenGenerator.generateToken(authentication);

        return ResponseEntity.ok(new LoginResponse(
                "CLIENTE",
                cliente.getId(),
                cliente.getNombre(),
                cliente.getApellido(),
                cliente.getEmail(),
                appJwt
        ));
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
