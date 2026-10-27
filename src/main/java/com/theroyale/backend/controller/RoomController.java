package com.theroyale.backend.controller;

import com.theroyale.backend.service.HabitacionService;
import com.theroyale.backend.service.TipoHabitacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// ===== Catálogo público de habitaciones (Thymeleaf): listado de tipos y detalle de cada uno =====
// Los cálculos de "precio desde" y disponibles por tipo los hace HabitacionService.
@Controller
public class RoomController {

    @Autowired
    private TipoHabitacionService tipoHabitacionService;

    @Autowired
    private HabitacionService habitacionService;

    @GetMapping("/rooms")
    public String listarRooms(Model model) {
        model.addAttribute("tiposHabitacion", tipoHabitacionService.listarTodos());
        model.addAttribute("preciosDesdePorTipo", habitacionService.preciosDesdePorTipo());
        model.addAttribute("habitacionesDisponiblesPorTipo", habitacionService.disponiblesPorTipo());
        return "rooms-lista";
    }

    @GetMapping("/rooms/{id}")
    public String mostrarDetalleRoom(@PathVariable Long id, Model model) {
        model.addAttribute("tipoHabitacion", tipoHabitacionService.obtenerPorId(id));
        model.addAttribute("habitaciones", habitacionService.listarPorTipoHabitacion(id));
        model.addAttribute("precioDesde", habitacionService.precioDesde(id));
        model.addAttribute("habitacionesDisponibles", habitacionService.contarDisponibles(id));
        return "room-detalle";
    }
}
