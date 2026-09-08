package com.theroyale.backend.controller;

import com.theroyale.backend.model.TipoHabitacion;
import com.theroyale.backend.service.TipoHabitacionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/tipos-habitacion")
public class TipoHabitacionController {

    private final TipoHabitacionService tipoHabitacionService;

    public TipoHabitacionController(TipoHabitacionService tipoHabitacionService) {
        this.tipoHabitacionService = tipoHabitacionService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("tipos", tipoHabitacionService.listarTodos());
        return "admin/tipos-habitacion-lista";
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioCreacion(Model model) {
        model.addAttribute("tipoHabitacion", new TipoHabitacion());
        return "admin/tipos-habitacion-formulario";
    }

    @GetMapping("/{id}/editar")
    public String mostrarFormularioEdicion(@PathVariable Long id, Model model) {
        try {
            model.addAttribute("tipoHabitacion", tipoHabitacionService.obtenerPorId(id));
            return "admin/tipos-habitacion-formulario";
        } catch (RuntimeException ex) {
            model.addAttribute("tipos", tipoHabitacionService.listarTodos());
            model.addAttribute("error", ex.getMessage());
            return "admin/tipos-habitacion-lista";
        }
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute TipoHabitacion tipoHabitacion, Model model) {
        try {
            tipoHabitacionService.guardar(tipoHabitacion);
            return "redirect:/admin/tipos-habitacion";
        } catch (RuntimeException ex) {
            model.addAttribute("tipoHabitacion", tipoHabitacion);
            model.addAttribute("error", ex.getMessage());
            return "admin/tipos-habitacion-formulario";
        }
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, Model model) {
        try {
            tipoHabitacionService.eliminar(id);
            return "redirect:/admin/tipos-habitacion";
        } catch (RuntimeException ex) {
            model.addAttribute("tipos", tipoHabitacionService.listarTodos());
            model.addAttribute("error", ex.getMessage());
            return "admin/tipos-habitacion-lista";
        }
    }
}
