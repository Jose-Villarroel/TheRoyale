package com.theroyale.backend.api;

import com.theroyale.backend.model.Habitacion;
import com.theroyale.backend.service.HabitacionService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// ===== API REST consumida por habitacion.service.ts del frontend Angular =====
@RestController
@RequestMapping("/api/habitaciones")
@CrossOrigin(origins = "http://localhost:4200")
public class HabitacionRestController {

    @Autowired
    private HabitacionService habitacionService;

    @Autowired
    private TipoHabitacionService tipoHabitacionService;

    // GET /api/habitaciones?tipoHabitacionId=1 -> solo las de ese tipo (lo que pide el detalle de room)
    @GetMapping
    public List<Habitacion> listar(@RequestParam(required = false) Long tipoHabitacionId) {
        if (tipoHabitacionId == null) {
            return habitacionService.listarOrdenadas();
        }
        return habitacionService.listarPorTipoHabitacion(tipoHabitacionId);
    }

    @GetMapping("/{id}")
    public Habitacion obtenerPorId(@PathVariable Long id) {
        return habitacionService.obtenerPorId(id);
    }

    @PostMapping
    public ResponseEntity<Habitacion> crear(@RequestBody Habitacion habitacion) {
        habitacion.setId(null);
        resolverTipoHabitacion(habitacion);
        return ResponseEntity.status(HttpStatus.CREATED).body(habitacionService.guardar(habitacion));
    }

    @PutMapping("/{id}")
    public Habitacion actualizar(@PathVariable Long id, @RequestBody Habitacion habitacion) {
        habitacion.setId(id);
        resolverTipoHabitacion(habitacion);
        return habitacionService.guardar(habitacion);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        habitacionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // El tipoHabitacion del JSON llega como entidad detached (normalmente solo con el id),
    // asi que se vuelve a leer de la base antes de guardar para que Hibernate lo gestione.
    private void resolverTipoHabitacion(Habitacion habitacion) {
        if (habitacion.getTipoHabitacion() == null || habitacion.getTipoHabitacion().getId() == null) {
            throw new IllegalArgumentException("Room type id is required.");
        }
        habitacion.setTipoHabitacion(
                tipoHabitacionService.obtenerPorId(habitacion.getTipoHabitacion().getId()));
    }
}
