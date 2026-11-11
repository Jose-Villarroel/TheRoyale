package com.theroyale.backend.api;

import com.theroyale.backend.dto.CrearReservaRequest;
import com.theroyale.backend.dto.ReservaResponse;
import com.theroyale.backend.model.EstadoReserva;
import com.theroyale.backend.service.ReservaService;
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

// ===== API REST consumida por reserva.service.ts del frontend Angular =====
// Todas las respuestas son ReservaResponse: cliente, habitación y tipo van aplanados, sin objetos anidados.
@RestController
@RequestMapping("/api/reservas")
@CrossOrigin(origins = "http://localhost:4200")
public class ReservaRestController {

    @Autowired
    private ReservaService reservaService;

    // ===== Todas las reservas; con ?estado= se filtra por estado =====
    // Con ?clienteId= se devuelven solo las de ese cliente, y ?activas= las separa
    // entre vigentes (true) e historial (false) para el portal del huesped.
    @GetMapping
    public List<ReservaResponse> listar(@RequestParam(required = false) EstadoReserva estado,
                                @RequestParam(required = false) Long clienteId,
                                @RequestParam(required = false) Boolean activas) {
        var reservas = clienteId != null
                ? reservaService.listarPorCliente(clienteId, activas)
                : reservaService.listar(estado);
        return reservas.stream().map(ReservaResponse::from).toList();
    }

    // ===== Buscar la reserva activa por numero de habitacion (portal operador) =====
    // El operador ingresa el numero (ej. "701") y recibe la reserva + datos del huesped
    // para confirmar que no haya equivocaciones antes de agregar consumos.
    @GetMapping("/buscar-habitacion")
    public ReservaResponse buscarPorHabitacion(@RequestParam String numero) {
        return ReservaResponse.from(reservaService.buscarActivaPorNumeroHabitacion(numero));
    }

    @GetMapping("/{id}")
    public ReservaResponse obtenerPorId(@PathVariable Long id) {
        return ReservaResponse.from(reservaService.obtenerPorId(id));
    }

    // ===== Crea una reserva: el sistema asigna una habitación libre del tipo pedido =====
    @PostMapping
    public ResponseEntity<ReservaResponse> crear(@RequestBody CrearReservaRequest solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ReservaResponse.from(reservaService.crear(solicitud)));
    }

    // ===== Reescribe la reserva con los datos nuevos, solo si hay disponibilidad =====
    @PutMapping("/{id}")
    public ReservaResponse modificar(@PathVariable Long id, @RequestBody CrearReservaRequest solicitud) {
        return ReservaResponse.from(reservaService.modificar(id, solicitud));
    }

    // ===== Transiciones del ciclo de vida, equivalentes a los POST de /operator =====
    // Los errores de negocio (estado invalido, saldo pendiente, fechas) los traduce
    // ApiExceptionHandler: IllegalState -> 409, IllegalArgument -> 400.
    @PostMapping("/{id}/confirmar")
    public ReservaResponse confirmar(@PathVariable Long id) {
        return ReservaResponse.from(reservaService.confirmar(id));
    }

    @PostMapping("/{id}/cancelar")
    public ReservaResponse cancelar(@PathVariable Long id) {
        return ReservaResponse.from(reservaService.cancelar(id));
    }

    @PostMapping("/{id}/check-in")
    public ReservaResponse checkIn(@PathVariable Long id) {
        return ReservaResponse.from(reservaService.hacerCheckIn(id));
    }

    @PostMapping("/{id}/check-out")
    public ReservaResponse checkOut(@PathVariable Long id) {
        return ReservaResponse.from(reservaService.hacerCheckOut(id));
    }

    // ===== Borra la reserva y su cuenta; 409 si la estadía está EN_CURSO, 404 si no existe =====
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        reservaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
