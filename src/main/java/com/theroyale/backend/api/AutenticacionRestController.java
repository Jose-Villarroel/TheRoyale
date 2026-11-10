package com.theroyale.backend.api;

import com.theroyale.backend.dto.GoogleAuthRequest;
import com.theroyale.backend.dto.LoginRequest;
import com.theroyale.backend.dto.LoginResponse;
import com.theroyale.backend.service.AutenticacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// ===== API REST consumida por auth.service.ts del frontend Angular =====
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AutenticacionRestController {

    @Autowired
    private AutenticacionService autenticacionService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> iniciarSesion(@RequestBody LoginRequest credenciales) {
        return autenticacionService.login(credenciales.email(), credenciales.password())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PostMapping("/google")
    public ResponseEntity<LoginResponse> iniciarSesionConGoogle(@RequestBody GoogleAuthRequest request) {
        if (request.email() == null || request.email().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        LoginResponse response = autenticacionService.loginConGoogle(
                request.email(),
                request.nombre(),
                request.apellido()
        );
        return ResponseEntity.ok(response);
    }
}
