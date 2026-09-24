package com.theroyale.backend.controller;

import com.theroyale.backend.model.Habitacion;
import com.theroyale.backend.service.HabitacionService;
import com.theroyale.backend.service.TipoHabitacionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    public String listar(@RequestParam(required = false) String error, Model model) {
        model.addAttribute("error", error);
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
        model.addAttribute("habitacion", habitacionService.obtenerPorId(id));
        model.addAttribute("tiposHabitacion", tipoHabitacionService.listarTodos());
        return "admin/habitaciones-formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Habitacion habitacion,
                          @RequestParam(required = false) Long tipoHabitacionId,
                          Model model) {
        try {
            if (tipoHabitacionId != null) {
                habitacion.setTipoHabitacion(tipoHabitacionService.obtenerPorId(tipoHabitacionId));
            }
            habitacionService.guardar(habitacion);
            return "redirect:/admin/habitaciones";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("habitacion", habitacion);
            model.addAttribute("tiposHabitacion", tipoHabitacionService.listarTodos());
            model.addAttribute("error", ex.getMessage());
            return "admin/habitaciones-formulario";
        }
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            habitacionService.eliminar(id);
        } catch (IllegalStateException ex) {
            redirectAttributes.addAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/habitaciones";
    }
}
