package com.theroyale.backend.api;

import com.theroyale.backend.dto.ServicioResponseDto;
import com.theroyale.backend.model.Servicio;
import com.theroyale.backend.service.InterfaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ===== API REST de servicios del hotel =====
@RestController
@RequestMapping("/api/servicios")
public class ServicioRestController {

    @Autowired
    private InterfaceService servicioService;

    @GetMapping
    public List<ServicioResponseDto> listar() {
        return servicioService.listarServicios().stream()
                .map(ServicioResponseDto::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ServicioResponseDto obtenerPorId(@PathVariable Long id) {
        return ServicioResponseDto.from(servicioService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<ServicioResponseDto> crear(@RequestBody Servicio servicio) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ServicioResponseDto.from(servicioService.crear(servicio)));
    }

    @PutMapping("/{id}")
    public ServicioResponseDto actualizar(@PathVariable Long id, @RequestBody Servicio servicio) {
        return ServicioResponseDto.from(servicioService.actualizar(id, servicio));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        servicioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
