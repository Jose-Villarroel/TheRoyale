package com.theroyale.backend.api;

import com.theroyale.backend.dto.TipoHabitacionResponseDto;
import com.theroyale.backend.model.TipoHabitacion;
import com.theroyale.backend.service.TipoHabitacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ===== API REST de tipos de habitación =====
@RestController
@RequestMapping("/api/tipos-habitacion")
public class TipoHabitacionRestController {

    @Autowired
    private TipoHabitacionService tipoHabitacionService;

    @GetMapping
    public List<TipoHabitacionResponseDto> listar() {
        return tipoHabitacionService.listarTodos().stream()
                .map(TipoHabitacionResponseDto::from)
                .toList();
    }

    @GetMapping("/{id}")
    public TipoHabitacionResponseDto obtenerPorId(@PathVariable Long id) {
        return TipoHabitacionResponseDto.from(tipoHabitacionService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<TipoHabitacionResponseDto> crear(@RequestBody TipoHabitacion tipoHabitacion) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TipoHabitacionResponseDto.from(tipoHabitacionService.crear(tipoHabitacion)));
    }

    @PutMapping("/{id}")
    public TipoHabitacionResponseDto actualizar(@PathVariable Long id, @RequestBody TipoHabitacion tipoHabitacion) {
        return TipoHabitacionResponseDto.from(tipoHabitacionService.actualizar(id, tipoHabitacion));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        tipoHabitacionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
