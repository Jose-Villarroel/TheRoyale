package com.theroyale.backend.api;

import com.theroyale.backend.model.TipoHabitacion;
import com.theroyale.backend.service.TipoHabitacionService;
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

// ===== API REST consumida por tipo-habitacion.service.ts del frontend Angular =====
@RestController
@RequestMapping("/api/tipos-habitacion")
@CrossOrigin(origins = "http://localhost:4200")
public class TipoHabitacionRestController {

    @Autowired
    private TipoHabitacionService tipoHabitacionService;

    @GetMapping
    public List<TipoHabitacion> listar() {
        return tipoHabitacionService.listarTodos();
    }

    @GetMapping("/{id}")
    public TipoHabitacion obtenerPorId(@PathVariable Long id) {
        return tipoHabitacionService.obtenerPorId(id);
    }

    @PostMapping
    public ResponseEntity<TipoHabitacion> crear(@RequestBody TipoHabitacion tipoHabitacion) {
        tipoHabitacion.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tipoHabitacionService.guardar(tipoHabitacion));
    }

    @PutMapping("/{id}")
    public TipoHabitacion actualizar(@PathVariable Long id, @RequestBody TipoHabitacion tipoHabitacion) {
        tipoHabitacion.setId(id);
        return tipoHabitacionService.guardar(tipoHabitacion);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        tipoHabitacionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
