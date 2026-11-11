package com.theroyale.backend.api;

import com.theroyale.backend.dto.CrearResenaRequest;
import com.theroyale.backend.dto.ResenaResponse;
import com.theroyale.backend.dto.ResumenResenasResponse;
import com.theroyale.backend.model.Resena;
import com.theroyale.backend.service.ResenaService;
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

// ===== API REST de reseñas consumida por resena.service.ts del frontend Angular =====
// Las reseñas salen como ResenaResponse (aplanadas y sin datos privados del autor).
// Errores vía ApiExceptionHandler: 400 datos inválidos, 403 reserva o reseña ajena,
// 404 no existe, 409 estado de la reserva o reseña duplicada.
@RestController
@RequestMapping("/api/resenas")
@CrossOrigin(origins = "http://localhost:4200")
public class ResenaRestController {

    @Autowired
    private ResenaService resenaService;

    // ===== Listados según los parámetros, en este orden de precedencia =====
    // 1. incluirOcultas=true -> moderación del admin: todas (visibles y ocultas), las más recientes
    //    primero; tipoHabitacionId y calificacion actúan como filtros opcionales.
    // 2. clienteId           -> las reseñas de ese cliente, incluidas las ocultas (se ignora el tipo).
    // 3. tipoHabitacionId    -> público: solo las visibles de ese tipo.
    // Sin ninguno de los tres -> 400.
    @GetMapping
    public List<ResenaResponse> listar(@RequestParam(required = false) Long tipoHabitacionId,
                                       @RequestParam(required = false) Long clienteId,
                                       @RequestParam(required = false) Boolean incluirOcultas,
                                       @RequestParam(required = false) Integer calificacion) {
        List<Resena> resenas;
        if (Boolean.TRUE.equals(incluirOcultas)) {
            resenas = resenaService.listarParaModeracion(tipoHabitacionId, calificacion);
        } else if (clienteId != null) {
            resenas = resenaService.listarPorCliente(clienteId);
        } else if (tipoHabitacionId != null) {
            resenas = resenaService.listarPorTipo(tipoHabitacionId);
        } else {
            throw new IllegalArgumentException("Specify tipoHabitacionId, clienteId or incluirOcultas=true.");
        }
        return resenas.stream().map(ResenaResponse::from).toList();
    }

    // ===== Resumen de un tipo (página /rooms/:id) =====
    @GetMapping(value = "/resumen", params = "tipoHabitacionId")
    public ResumenResenasResponse resumen(@RequestParam Long tipoHabitacionId) {
        return resenaService.resumen(tipoHabitacionId);
    }

    // ===== Resumen de todos los tipos, incluidos los que no tienen reseñas (listado /rooms) =====
    @GetMapping("/resumen")
    public List<ResumenResenasResponse> resumenTodos() {
        return resenaService.resumenTodos();
    }

    @GetMapping("/{id}")
    public ResenaResponse obtenerPorId(@PathVariable Long id) {
        return ResenaResponse.from(resenaService.obtenerPorId(id));
    }

    // ===== El huésped reseña una estadía FINALIZADA suya =====
    @PostMapping
    public ResponseEntity<ResenaResponse> crear(@RequestBody CrearResenaRequest solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResenaResponse.from(resenaService.crear(solicitud)));
    }

    // ===== El autor edita estrellas, título y comentario (reservaId del cuerpo se ignora) =====
    @PutMapping("/{id}")
    public ResenaResponse actualizar(@PathVariable Long id, @RequestBody CrearResenaRequest cambios) {
        return ResenaResponse.from(resenaService.actualizar(id, cambios));
    }

    // ===== El autor elimina su reseña; clienteId identifica al autor mientras no haya JWT =====
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @RequestParam Long clienteId) {
        resenaService.eliminar(id, clienteId);
        return ResponseEntity.noContent().build();
    }

    // ===== Moderación del admin =====
    @PostMapping("/{id}/ocultar")
    public ResenaResponse ocultar(@PathVariable Long id) {
        return ResenaResponse.from(resenaService.ocultar(id));
    }

    @PostMapping("/{id}/mostrar")
    public ResenaResponse mostrar(@PathVariable Long id) {
        return ResenaResponse.from(resenaService.mostrar(id));
    }
}
