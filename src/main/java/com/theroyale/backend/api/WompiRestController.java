package com.theroyale.backend.api;

import com.theroyale.backend.dto.wompi.WompiPrepararRequest;
import com.theroyale.backend.dto.wompi.WompiPrepararResponse;
import com.theroyale.backend.dto.wompi.WompiVerificarRequest;
import com.theroyale.backend.dto.wompi.WompiVerificarResponse;
import com.theroyale.backend.service.WompiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pagos/wompi")
public class WompiRestController {

    @Autowired
    private WompiService wompiService;

    @PostMapping("/preparar")
    public ResponseEntity<WompiPrepararResponse> prepararPago(@RequestBody WompiPrepararRequest request) {
        return ResponseEntity.ok(wompiService.prepararPago(request));
    }

    @PostMapping("/verificar")
    public ResponseEntity<WompiVerificarResponse> verificarPago(@RequestBody WompiVerificarRequest request) {
        return ResponseEntity.ok(wompiService.verificarYConfirmar(request.transactionId(), request.reservaId()));
    }
}
