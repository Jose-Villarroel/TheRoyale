package com.theroyale.backend.api;

import com.theroyale.backend.dto.PlatoMenuRequest;
import com.theroyale.backend.dto.PlatoMenuResponse;
import com.theroyale.backend.model.CategoriaPlato;
import com.theroyale.backend.model.EtiquetaPlato;
import com.theroyale.backend.model.RestauranteHotel;
import com.theroyale.backend.service.PlatoMenuService;
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

// ===== API REST de la carta de los restaurantes, consumida por plato-menu.service.ts del frontend =====
// Los filtros del GET son opcionales; un valor de enum desconocido responde 400 (ApiExceptionHandler).
@RestController
@RequestMapping("/api/menu")
@CrossOrigin(origins = "http://localhost:4200")
public class PlatoMenuRestController {

    @Autowired
    private PlatoMenuService platoMenuService;

    @GetMapping
    public List<PlatoMenuResponse> listar(@RequestParam(required = false) RestauranteHotel restaurante,
                                          @RequestParam(required = false) CategoriaPlato categoria,
                                          @RequestParam(required = false) EtiquetaPlato etiqueta,
                                          @RequestParam(defaultValue = "false") boolean soloDisponibles) {
        return platoMenuService.listar(restaurante, categoria, etiqueta, soloDisponibles).stream()
                .map(PlatoMenuResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public PlatoMenuResponse obtenerPorId(@PathVariable Long id) {
        return PlatoMenuResponse.from(platoMenuService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<PlatoMenuResponse> crear(@RequestBody PlatoMenuRequest solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PlatoMenuResponse.from(platoMenuService.crear(solicitud)));
    }

    @PutMapping("/{id}")
    public PlatoMenuResponse actualizar(@PathVariable Long id, @RequestBody PlatoMenuRequest solicitud) {
        return PlatoMenuResponse.from(platoMenuService.actualizar(id, solicitud));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        platoMenuService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
