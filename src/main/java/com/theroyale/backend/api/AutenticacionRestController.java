package com.theroyale.backend.api;

import com.theroyale.backend.dto.LoginRequest;
import com.theroyale.backend.dto.LoginResponse;
import com.theroyale.backend.service.AutenticacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// ===== API REST consumida por auth.service.ts del frontend Angular =====
// El POST /login de AutenticacionController sigue sirviendo al formulario Thymeleaf;
// este endpoint es el equivalente en JSON para la SPA. Ambos delegan en AutenticacionService.login.
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
}
