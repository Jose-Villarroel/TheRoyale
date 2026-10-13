package com.theroyale.backend.api;

import com.theroyale.backend.model.Cliente;
import com.theroyale.backend.model.Operador;
import com.theroyale.backend.service.AutenticacionService;
import com.theroyale.backend.service.OperadorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

// ===== API REST consumida por auth.service.ts del frontend Angular =====
// El POST /login de AutenticacionController sigue sirviendo al formulario Thymeleaf;
// este endpoint es el equivalente en JSON para la SPA.
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AutenticacionRestController {

    @Autowired
    private AutenticacionService autenticacionService;

    @Autowired
    private OperadorService operadorService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> iniciarSesion(@RequestBody LoginRequest credenciales) {
        Optional<Cliente> cliente =
                autenticacionService.autenticar(credenciales.email(), credenciales.password());
        if (cliente.isPresent()) {
            Cliente autenticado = cliente.get();
            return ResponseEntity.ok(new LoginResponse(
                    "CLIENTE",
                    autenticado.getId(),
                    autenticado.getNombre(),
                    autenticado.getApellido(),
                    autenticado.getEmail()));
        }

        Optional<Operador> operador =
                operadorService.autenticar(credenciales.email(), credenciales.password());
        if (operador.isPresent()) {
            Operador autenticado = operador.get();
            // Operador no tiene apellido; se manda vacio para no romper la interfaz del frontend.
            return ResponseEntity.ok(new LoginResponse(
                    "OPERADOR",
                    autenticado.getId(),
                    autenticado.getNombre(),
                    "",
                    autenticado.getEmail()));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
