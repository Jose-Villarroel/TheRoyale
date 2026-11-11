package com.theroyale.backend.api;

import com.theroyale.backend.dto.HabitacionRequest;
import com.theroyale.backend.dto.HabitacionResponse;
import com.theroyale.backend.service.HabitacionService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// ===== API REST consumida por habitacion.service.ts del frontend Angular =====
// Entra y sale en DTOs planos (HabitacionRequest / HabitacionResponse): el tipo de habitación
// viaja como tipoHabitacionId y HabitacionService se encarga de resolverlo y validarlo.
@RestController
@RequestMapping("/api/habitaciones")
@CrossOrigin(origins = "http://localhost:4200")
public class HabitacionRestController {

    @Autowired
    private HabitacionService habitacionService;

    // GET /api/habitaciones?tipoHabitacionId=1 -> solo las de ese tipo (lo que pide el detalle de room)
    @GetMapping
    public List<HabitacionResponse> listar(@RequestParam(required = false) Long tipoHabitacionId) {
        var habitaciones = tipoHabitacionId == null
                ? habitacionService.listarOrdenadas()
                : habitacionService.listarPorTipoHabitacion(tipoHabitacionId);
        return habitaciones.stream().map(HabitacionResponse::from).toList();
    }

    @GetMapping("/{id}")
    public HabitacionResponse obtenerPorId(@PathVariable Long id) {
        return HabitacionResponse.from(habitacionService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<HabitacionResponse> crear(@RequestBody HabitacionRequest solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(HabitacionResponse.from(habitacionService.crear(solicitud)));
    }

    @PutMapping("/{id}")
    public HabitacionResponse actualizar(@PathVariable Long id, @RequestBody HabitacionRequest solicitud) {
        return HabitacionResponse.from(habitacionService.actualizar(id, solicitud));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        habitacionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
