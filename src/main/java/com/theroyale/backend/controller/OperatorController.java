package com.theroyale.backend.controller;

import com.theroyale.backend.model.EstadoHabitacion;
import com.theroyale.backend.model.EstadoReserva;
import com.theroyale.backend.service.CuentaService;
import com.theroyale.backend.service.HabitacionService;
import com.theroyale.backend.service.InterfaceService;
import com.theroyale.backend.service.OperadorService;
import com.theroyale.backend.service.ReservaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;

// ===== Panel del operario: habitaciones, reservas y cuenta de consumo de cada estadia =====
// Por ahora /operator es accesible sin iniciar sesion. Si el operador si entro por /login, sus acciones quedan
// a su nombre; si no, se atribuyen al operador predeterminado (ver OperadorService.obtenerOPredeterminado).
@Controller
@RequestMapping("/operator")
public class OperatorController {

    // Nombre con el que AutenticacionController guarda el id del operador en la sesion al iniciar sesion
    public static final String ATRIBUTO_SESION = "operadorId";

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
    public String dashboard(@SessionAttribute(name = ATRIBUTO_SESION, required = false) Long operadorId, Model model) {
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
    public String habitaciones(Model model) {
        model.addAttribute("habitaciones", habitacionService.listarOrdenadas());
        model.addAttribute("estados", EstadoHabitacion.values());
        return "operator/habitaciones";
    }

    @PostMapping("/habitaciones/{id}/estado")
    public String cambiarEstadoHabitacion(@PathVariable Long id,
                                          @RequestParam EstadoHabitacion estado,
                                          RedirectAttributes redirectAttributes) {
        ejecutar(redirectAttributes, "Room status updated.", () -> habitacionService.cambiarEstado(id, estado));
        return "redirect:/operator/habitaciones";
    }

    // ===== Reservas =====
    @GetMapping("/reservas")
    public String reservas(@RequestParam(required = false) EstadoReserva estado, Model model) {
        model.addAttribute("reservas", reservaService.listar(estado));
        model.addAttribute("estados", EstadoReserva.values());
        model.addAttribute("conteos", reservaService.contarTodasPorEstado());
        model.addAttribute("estadoFiltro", estado);
        return "operator/reservas";
    }

    @GetMapping("/reservas/{id}")
    public String detalleReserva(@PathVariable Long id, Model model) {
        model.addAttribute("reserva", reservaService.obtenerPorId(id));
        model.addAttribute("cuenta", cuentaService.buscarPorReserva(id).orElse(null));
        model.addAttribute("servicios", servicioService.listarServicios());
        model.addAttribute("metodosPago", METODOS_PAGO);
        return "operator/reserva-detalle";
    }

    @PostMapping("/reservas/{id}/confirmar")
    public String confirmar(@PathVariable Long id,
                            @RequestParam(defaultValue = "detalle") String volver,
                            @SessionAttribute(name = ATRIBUTO_SESION, required = false) Long operadorId,
                            RedirectAttributes redirectAttributes) {
        ejecutar(redirectAttributes, "Reservation confirmed.", () -> reservaService.confirmar(id, operadorId));
        return destino(id, volver);
    }

    @PostMapping("/reservas/{id}/cancelar")
    public String cancelar(@PathVariable Long id,
                           @RequestParam(defaultValue = "detalle") String volver,
                           @SessionAttribute(name = ATRIBUTO_SESION, required = false) Long operadorId,
                           RedirectAttributes redirectAttributes) {
        ejecutar(redirectAttributes, "Reservation cancelled.", () -> reservaService.cancelar(id, operadorId));
        return destino(id, volver);
    }

    @PostMapping("/reservas/{id}/check-in")
    public String checkIn(@PathVariable Long id,
                          @RequestParam(defaultValue = "detalle") String volver,
                          @SessionAttribute(name = ATRIBUTO_SESION, required = false) Long operadorId,
                          RedirectAttributes redirectAttributes) {
        ejecutar(redirectAttributes, "Check-in completed. The guest account is open.", () -> reservaService.hacerCheckIn(id, operadorId));
        return destino(id, volver);
    }

    @PostMapping("/reservas/{id}/check-out")
    public String checkOut(@PathVariable Long id,
                           @RequestParam(defaultValue = "detalle") String volver,
                           @SessionAttribute(name = ATRIBUTO_SESION, required = false) Long operadorId,
                           RedirectAttributes redirectAttributes) {
        ejecutar(redirectAttributes, "Check-out completed. The room is available again.", () -> reservaService.hacerCheckOut(id, operadorId));
        return destino(id, volver);
    }

    // ===== Cuenta de la reserva =====
    @PostMapping("/reservas/{id}/consumos")
    public String agregarConsumo(@PathVariable Long id,
                                 @RequestParam Long servicioId,
                                 @RequestParam Integer cantidad,
                                 @SessionAttribute(name = ATRIBUTO_SESION, required = false) Long operadorId,
                                 RedirectAttributes redirectAttributes) {
        ejecutar(redirectAttributes, "Charge added to the account.", () -> cuentaService.agregarConsumo(id, servicioId, cantidad, operadorId));
        return destino(id, "detalle");
    }

    @PostMapping("/reservas/{id}/consumos/{itemId}/eliminar")
    public String eliminarConsumo(@PathVariable Long id,
                                  @PathVariable Long itemId,
                                  RedirectAttributes redirectAttributes) {
        ejecutar(redirectAttributes, "Charge removed from the account.", () -> cuentaService.eliminarConsumo(id, itemId));
        return destino(id, "detalle");
    }

    @PostMapping("/reservas/{id}/pagos")
    public String registrarPago(@PathVariable Long id,
                                @RequestParam BigDecimal monto,
                                @RequestParam String metodoPago,
                                @SessionAttribute(name = ATRIBUTO_SESION, required = false) Long operadorId,
                                RedirectAttributes redirectAttributes) {
        ejecutar(redirectAttributes, "Payment registered.", () -> cuentaService.registrarPago(id, monto, metodoPago, operadorId));
        return destino(id, "detalle");
    }

    @PostMapping("/logout")
    public String cerrarSesion(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    // ===== Utilidades =====
    // Los errores de negocio (transicion invalida, saldo, etc.) se muestran como alerta en la misma vista.
    private void ejecutar(RedirectAttributes redirectAttributes, String exito, Supplier<?> accion) {
        try {
            accion.get();
            redirectAttributes.addFlashAttribute("mensaje", exito);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
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
