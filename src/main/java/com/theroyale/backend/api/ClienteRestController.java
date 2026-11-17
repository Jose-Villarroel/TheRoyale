package com.theroyale.backend.api;

import com.theroyale.backend.dto.ClienteResponseDto;
import com.theroyale.backend.model.Cliente;
import com.theroyale.backend.security.UsuarioActualService;
import com.theroyale.backend.service.ClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ===== API REST de gestión de clientes =====
@RestController
@RequestMapping("/api/clientes")
public class ClienteRestController {

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private UsuarioActualService usuarioActualService;

    @GetMapping
    public List<ClienteResponseDto> listar() {
        return clienteService.listarTodos().stream()
                .map(ClienteResponseDto::from)
                .toList();
    }

    // Un CLIENTE solo puede ver su propio perfil (403 si pide otro id)
    @GetMapping("/{id}")
    public ClienteResponseDto obtenerPorId(@PathVariable Long id) {
        usuarioActualService.exigirClientePropio(id);
        return ClienteResponseDto.from(clienteService.obtenerPorId(id));
    }

    // Endpoint details basado en SecurityContextHolder (como en la clase del profesor)
    @GetMapping("/details")
    public ResponseEntity<ClienteResponseDto> obtenerDetalles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return clienteService.buscarPorEmail(auth.getName())
                .map(c -> ResponseEntity.ok(ClienteResponseDto.from(c)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping
    public ResponseEntity<ClienteResponseDto> crear(@RequestBody Cliente cliente) {
        Cliente creado = clienteService.crear(cliente);
        return ResponseEntity.status(HttpStatus.CREATED).body(ClienteResponseDto.from(creado));
    }

    // Un CLIENTE solo edita su propio perfil; el admin edita cualquiera
    @PutMapping("/{id}")
    public ClienteResponseDto actualizar(@PathVariable Long id, @RequestBody Cliente cliente) {
        usuarioActualService.exigirClientePropio(id);
        return ClienteResponseDto.from(clienteService.actualizar(id, cliente));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        clienteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
