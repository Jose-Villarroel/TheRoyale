package com.theroyale.backend.api;

import com.theroyale.backend.dto.CrearResenaRequest;
import com.theroyale.backend.dto.ResenaResponse;
import com.theroyale.backend.dto.ResumenResenasResponse;
import com.theroyale.backend.service.ResenaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resenas")
@CrossOrigin(origins = "http://localhost:4200")
public class ResenaRestController {

    @Autowired
    private ResenaService resenaService;

    @GetMapping("/tipo/{tipoId}")
    public ResponseEntity<ResumenResenasResponse> obtenerResumenPorTipo(@PathVariable Long tipoId) {
        return ResponseEntity.ok(resenaService.obtenerResumenPorTipo(tipoId));
    }

    @GetMapping("/recientes")
    public ResponseEntity<List<ResenaResponse>> listarRecientes() {
        return ResponseEntity.ok(resenaService.listarRecientes());
    }

    @PostMapping
    public ResponseEntity<ResenaResponse> crearResena(@RequestBody CrearResenaRequest request) {
        ResenaResponse response = resenaService.crearResena(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
