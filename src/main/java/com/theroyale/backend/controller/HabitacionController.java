package com.theroyale.backend.controller;

import com.theroyale.backend.model.Habitacion;
import com.theroyale.backend.service.HabitacionService;
import com.theroyale.backend.service.TipoHabitacionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/habitaciones")
public class HabitacionController {

    private final HabitacionService habitacionService;

    private final TipoHabitacionService tipoHabitacionService;

    public HabitacionController(HabitacionService habitacionService, TipoHabitacionService tipoHabitacionService) {
        this.habitacionService = habitacionService;
        this.tipoHabitacionService = tipoHabitacionService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("habitaciones", habitacionService.listarTodos());
        return "admin/habitaciones-lista";
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioCreacion(Model model) {
        model.addAttribute("habitacion", new Habitacion());
        model.addAttribute("tiposHabitacion", tipoHabitacionService.listarTodos());
        return "admin/habitaciones-formulario";
    }

    @GetMapping("/{id}/editar")
    public String mostrarFormularioEdicion(@PathVariable Long id, Model model) {
        try {
            model.addAttribute("habitacion", habitacionService.obtenerPorId(id));
            model.addAttribute("tiposHabitacion", tipoHabitacionService.listarTodos());
            return "admin/habitaciones-formulario";
        } catch (RuntimeException ex) {
            model.addAttribute("habitaciones", habitacionService.listarTodos());
            model.addAttribute("error", ex.getMessage());
            return "admin/habitaciones-lista";
        }
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Habitacion habitacion, @RequestParam Long tipoHabitacionId, Model model) {
        try {
            habitacion.setTipoHabitacion(tipoHabitacionService.obtenerPorId(tipoHabitacionId));
            habitacionService.guardar(habitacion);
            return "redirect:/admin/habitaciones";
        } catch (RuntimeException ex) {
            model.addAttribute("habitacion", habitacion);
            model.addAttribute("tiposHabitacion", tipoHabitacionService.listarTodos());
            model.addAttribute("error", ex.getMessage());
            return "admin/habitaciones-formulario";
        }
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, Model model) {
        try {
            habitacionService.eliminar(id);
            return "redirect:/admin/habitaciones";
        } catch (RuntimeException ex) {
            model.addAttribute("habitaciones", habitacionService.listarTodos());
            model.addAttribute("error", ex.getMessage());
            return "admin/habitaciones-lista";
        }
    }
}
