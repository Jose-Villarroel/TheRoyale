package com.theroyale.backend.controller;

import com.theroyale.backend.model.EstadoHabitacion;
import com.theroyale.backend.model.EstadoReserva;
import com.theroyale.backend.service.CuentaService;
import com.theroyale.backend.service.HabitacionService;
import com.theroyale.backend.service.InterfaceService;
import com.theroyale.backend.service.OperadorService;
import com.theroyale.backend.service.ReservaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;

// ===== Panel del operario: habitaciones, reservas y cuenta de consumo de cada estadia =====
// El operador que inicio sesion viaja en la URL como ?operadorId= (igual que ?clienteId= para los huespedes).
// Si no llega, las acciones se atribuyen al operador predeterminado (ver OperadorService.obtenerOPredeterminado).
@Controller
@RequestMapping("/operator")
public class OperatorController {

    private static final List<String> METODOS_PAGO = List.of("CASH", "CARD", "TRANSFER");

    private final HabitacionService habitacionService;
    private final ReservaService reservaService;
    private final CuentaService cuentaService;
    private final OperadorService operadorService;
    private final InterfaceService servicioService;

    public OperatorController(HabitacionService habitacionService,
                              ReservaService reservaService,
                              CuentaService cuentaService,
                              OperadorService operadorService,
                              InterfaceService servicioService) {
        this.habitacionService = habitacionService;
        this.reservaService = reservaService;
        this.cuentaService = cuentaService;
        this.operadorService = operadorService;
        this.servicioService = servicioService;
    }

    @GetMapping
    public String dashboard(@RequestParam(required = false) Long operadorId,
                            @RequestParam(required = false) String mensaje,
                            @RequestParam(required = false) String error,
                            Model model) {
        prepararVista(model, operadorId, mensaje, error);
        model.addAttribute("operador", operadorService.obtenerOPredeterminado(operadorId));
        model.addAttribute("habitacionesDisponibles", habitacionService.contarPorEstado(EstadoHabitacion.DISPONIBLE));
        model.addAttribute("habitacionesOcupadas", habitacionService.contarPorEstado(EstadoHabitacion.OCUPADA));
        model.addAttribute("habitacionesMantenimiento", habitacionService.contarPorEstado(EstadoHabitacion.MANTENIMIENTO));
        model.addAttribute("reservasPendientes", reservaService.contarPorEstado(EstadoReserva.PENDIENTE));
        model.addAttribute("reservasEnCurso", reservaService.contarPorEstado(EstadoReserva.EN_CURSO));
        model.addAttribute("cuentasAbiertas", cuentaService.contarAbiertas());
        model.addAttribute("llegadas", reservaService.llegadasDeHoy());
        model.addAttribute("salidas", reservaService.salidasDeHoy());
        return "operator/dashboard";
    }

    // ===== Habitaciones =====
    @GetMapping("/habitaciones")
    public String habitaciones(@RequestParam(required = false) Long operadorId,
                               @RequestParam(required = false) String mensaje,
                               @RequestParam(required = false) String error,
                               Model model) {
        prepararVista(model, operadorId, mensaje, error);
        model.addAttribute("habitaciones", habitacionService.listarOrdenadas());
        model.addAttribute("estados", EstadoHabitacion.values());
        return "operator/habitaciones";
    }

    @PostMapping("/habitaciones/{id}/estado")
    public String cambiarEstadoHabitacion(@PathVariable Long id,
                                          @RequestParam EstadoHabitacion estado,
                                          @RequestParam(required = false) Long operadorId,
                                          RedirectAttributes redirectAttributes) {
        ejecutar(redirectAttributes, operadorId, "Room status updated.", () -> habitacionService.cambiarEstado(id, estado));
        return "redirect:/operator/habitaciones";
    }

    // ===== Reservas =====
    @GetMapping("/reservas")
    public String reservas(@RequestParam(required = false) EstadoReserva estado,
                           @RequestParam(required = false) Long operadorId,
                           @RequestParam(required = false) String mensaje,
                           @RequestParam(required = false) String error,
                           Model model) {
        prepararVista(model, operadorId, mensaje, error);
        model.addAttribute("reservas", reservaService.listar(estado));
        model.addAttribute("estados", EstadoReserva.values());
        model.addAttribute("conteos", reservaService.contarTodasPorEstado());
        model.addAttribute("estadoFiltro", estado);
        return "operator/reservas";
    }

    @GetMapping("/reservas/{id}")
    public String detalleReserva(@PathVariable Long id,
                                 @RequestParam(required = false) Long operadorId,
                                 @RequestParam(required = false) String mensaje,
                                 @RequestParam(required = false) String error,
                                 Model model) {
        prepararVista(model, operadorId, mensaje, error);
        model.addAttribute("reserva", reservaService.obtenerPorId(id));
        model.addAttribute("cuenta", cuentaService.buscarPorReserva(id).orElse(null));
        model.addAttribute("servicios", servicioService.listarServicios());
        model.addAttribute("metodosPago", METODOS_PAGO);
        return "operator/reserva-detalle";
    }

    @PostMapping("/reservas/{id}/confirmar")
    public String confirmar(@PathVariable Long id,
                            @RequestParam(defaultValue = "detalle") String volver,
                            @RequestParam(required = false) Long operadorId,
                            RedirectAttributes redirectAttributes) {
        ejecutar(redirectAttributes, operadorId, "Reservation confirmed.", () -> reservaService.confirmar(id));
        return destino(id, volver);
    }

    @PostMapping("/reservas/{id}/cancelar")
    public String cancelar(@PathVariable Long id,
                           @RequestParam(defaultValue = "detalle") String volver,
                           @RequestParam(required = false) Long operadorId,
                           RedirectAttributes redirectAttributes) {
        ejecutar(redirectAttributes, operadorId, "Reservation cancelled.", () -> reservaService.cancelar(id));
        return destino(id, volver);
    }

    @PostMapping("/reservas/{id}/check-in")
    public String checkIn(@PathVariable Long id,
                          @RequestParam(defaultValue = "detalle") String volver,
                          @RequestParam(required = false) Long operadorId,
                          RedirectAttributes redirectAttributes) {
        ejecutar(redirectAttributes, operadorId, "Check-in completed. The guest account is open.", () -> reservaService.hacerCheckIn(id));
        return destino(id, volver);
    }

    @PostMapping("/reservas/{id}/check-out")
    public String checkOut(@PathVariable Long id,
                           @RequestParam(defaultValue = "detalle") String volver,
                           @RequestParam(required = false) Long operadorId,
                           RedirectAttributes redirectAttributes) {
        ejecutar(redirectAttributes, operadorId, "Check-out completed. The room is available again.", () -> reservaService.hacerCheckOut(id));
        return destino(id, volver);
    }

    // ===== Cuenta de la reserva =====
    @PostMapping("/reservas/{id}/consumos")
    public String agregarConsumo(@PathVariable Long id,
                                 @RequestParam Long servicioId,
                                 @RequestParam Integer cantidad,
                                 @RequestParam(required = false) Long operadorId,
                                 RedirectAttributes redirectAttributes) {
        ejecutar(redirectAttributes, operadorId, "Charge added to the account.", () -> cuentaService.agregarConsumo(id, servicioId, cantidad, operadorId));
        return destino(id, "detalle");
    }

    @PostMapping("/reservas/{id}/consumos/{itemId}/eliminar")
    public String eliminarConsumo(@PathVariable Long id,
                                  @PathVariable Long itemId,
                                  @RequestParam(required = false) Long operadorId,
                                  RedirectAttributes redirectAttributes) {
        ejecutar(redirectAttributes, operadorId, "Charge removed from the account.", () -> cuentaService.eliminarConsumo(id, itemId));
        return destino(id, "detalle");
    }

    @PostMapping("/reservas/{id}/pagos")
    public String registrarPago(@PathVariable Long id,
                                @RequestParam BigDecimal monto,
                                @RequestParam String metodoPago,
                                @RequestParam(required = false) Long operadorId,
                                RedirectAttributes redirectAttributes) {
        ejecutar(redirectAttributes, operadorId, "Payment registered.", () -> cuentaService.registrarPago(id, monto, metodoPago, operadorId));
        return destino(id, "detalle");
    }

    @PostMapping("/logout")
    public String cerrarSesion() {
        return "redirect:/login";
    }

    // ===== Utilidades =====
    // Pasa a la vista el operador actual (para propagarlo en links y formularios) y los avisos recibidos por URL.
    private void prepararVista(Model model, Long operadorId, String mensaje, String error) {
        model.addAttribute("operadorId", operadorId);
        model.addAttribute("mensaje", mensaje);
        model.addAttribute("error", error);
    }

    // Los errores de negocio (transicion invalida, saldo, etc.) se muestran como alerta en la misma vista.
    private void ejecutar(RedirectAttributes redirectAttributes, Long operadorId, String exito, Supplier<?> accion) {
        if (operadorId != null) {
            redirectAttributes.addAttribute("operadorId", operadorId);
        }
        try {
            accion.get();
            redirectAttributes.addAttribute("mensaje", exito);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            redirectAttributes.addAttribute("error", ex.getMessage());
        }
    }

    private String destino(Long reservaId, String volver) {
        return switch (volver) {
            case "lista" -> "redirect:/operator/reservas";
            case "dashboard" -> "redirect:/operator";
            default -> "redirect:/operator/reservas/" + reservaId;
        };
    }
}
