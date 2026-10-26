package com.theroyale.backend.api;

import com.theroyale.backend.dto.CrearReservaRequest;
import com.theroyale.backend.model.EstadoReserva;
import com.theroyale.backend.model.Reserva;
import com.theroyale.backend.service.ReservaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
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
    public List<Reserva> listar(@RequestParam(required = false) EstadoReserva estado,
                                @RequestParam(required = false) Long clienteId,
                                @RequestParam(required = false) Boolean activas) {
        if (clienteId != null) {
            return reservaService.listarPorCliente(clienteId, activas);
        }
        return reservaService.listar(estado);
    }

    // ===== Buscar la reserva activa por numero de habitacion (portal operador) =====
    // El operador ingresa el numero (ej. "701") y recibe la reserva + datos del huesped
    // para confirmar que no haya equivocaciones antes de agregar consumos.
    @GetMapping("/buscar-habitacion")
    public Reserva buscarPorHabitacion(@RequestParam String numero) {
        return reservaService.buscarActivaPorNumeroHabitacion(numero);
    }

    @GetMapping("/{id}")
    public Reserva obtenerPorId(@PathVariable Long id) {
        return reservaService.obtenerPorId(id);
    }

    // ===== Crea una reserva: el sistema asigna una habitación libre del tipo pedido =====
    @PostMapping
    public ResponseEntity<Reserva> crear(@RequestBody CrearReservaRequest solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reservaService.crear(solicitud));
    }

    // ===== Reescribe la reserva con los datos nuevos, solo si hay disponibilidad =====
    @PutMapping("/{id}")
    public Reserva modificar(@PathVariable Long id, @RequestBody CrearReservaRequest solicitud) {
        return reservaService.modificar(id, solicitud);
    }

    // ===== Transiciones del ciclo de vida, equivalentes a los POST de /operator =====
    // Los errores de negocio (estado invalido, saldo pendiente, fechas) los traduce
    // ApiExceptionHandler: IllegalState -> 409, IllegalArgument -> 400.
    @PostMapping("/{id}/confirmar")
    public Reserva confirmar(@PathVariable Long id) {
        return reservaService.confirmar(id);
    }

    @PostMapping("/{id}/cancelar")
    public Reserva cancelar(@PathVariable Long id) {
        return reservaService.cancelar(id);
    }

    @PostMapping("/{id}/check-in")
    public Reserva checkIn(@PathVariable Long id) {
        return reservaService.hacerCheckIn(id);
    }

    @PostMapping("/{id}/check-out")
    public Reserva checkOut(@PathVariable Long id) {
        return reservaService.hacerCheckOut(id);
    }
}