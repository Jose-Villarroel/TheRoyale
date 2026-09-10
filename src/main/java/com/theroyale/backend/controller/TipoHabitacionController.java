package com.theroyale.backend.controller;

import com.theroyale.backend.model.TipoHabitacion;
import com.theroyale.backend.service.TipoHabitacionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
        model.addAttribute("tipoHabitacion", tipoHabitacionService.obtenerPorId(id));
        return "admin/tipos-habitacion-formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute TipoHabitacion tipoHabitacion) {
        tipoHabitacionService.guardar(tipoHabitacion);
        return "redirect:/admin/tipos-habitacion";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            tipoHabitacionService.eliminar(id);
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/tipos-habitacion";
    }
}
