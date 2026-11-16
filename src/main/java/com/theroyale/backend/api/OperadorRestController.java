package com.theroyale.backend.api;

import com.theroyale.backend.dto.OperadorResponseDto;
import com.theroyale.backend.model.Operador;
import com.theroyale.backend.service.OperadorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ===== API REST del CRUD de operadores, consumida por el panel de administración =====
@RestController
@RequestMapping("/api/operadores")
public class OperadorRestController {

    @Autowired
    private OperadorService operadorService;

    @GetMapping
    public List<OperadorResponseDto> listar() {
        return operadorService.listarTodos().stream()
                .map(OperadorResponseDto::from)
                .toList();
    }

    @GetMapping("/{id}")
    public OperadorResponseDto obtenerPorId(@PathVariable Long id) {
        return OperadorResponseDto.from(operadorService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<OperadorResponseDto> crear(@RequestBody Operador operador) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(OperadorResponseDto.from(operadorService.crear(operador)));
    }

    @PutMapping("/{id}")
    public OperadorResponseDto actualizar(@PathVariable Long id, @RequestBody Operador operador) {
        return OperadorResponseDto.from(operadorService.actualizar(id, operador));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        operadorService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
