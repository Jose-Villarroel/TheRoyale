package com.theroyale.backend.api;

import com.theroyale.backend.model.Operador;
import com.theroyale.backend.service.OperadorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// ===== API REST del CRUD de operadores, consumida por el panel de administración =====
@RestController
@RequestMapping("/api/operadores")
@CrossOrigin(origins = "http://localhost:4200")
public class OperadorRestController {

    @Autowired
    private OperadorService operadorService;

    @GetMapping
    public List<Operador> listar() {
        return operadorService.listarTodos();
    }

    @GetMapping("/{id}")
    public Operador obtenerPorId(@PathVariable Long id) {
        return operadorService.obtenerPorId(id);
    }

    @PostMapping
    public ResponseEntity<Operador> crear(@RequestBody Operador operador) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(operadorService.crear(operador));
    }

    @PutMapping("/{id}")
    public Operador actualizar(@PathVariable Long id, @RequestBody Operador operador) {
        return operadorService.actualizar(id, operador);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        operadorService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
