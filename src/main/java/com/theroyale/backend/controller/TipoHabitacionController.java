package com.theroyale.backend.controller;

import com.theroyale.backend.model.TipoHabitacion;
import com.theroyale.backend.service.TipoHabitacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/tipos-habitacion")
public class TipoHabitacionController {

    @Autowired
    private TipoHabitacionService tipoHabitacionService;

    // ===== Listar todos los tipos de habitación =====
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("tipos", tipoHabitacionService.listarTodos());
        return "admin/tipos-habitacion-lista";
    }

    // ===== Mostrar el formulario para crear uno nuevo =====
    @GetMapping("/nuevo")
    public String mostrarFormularioCreacion(Model model) {
        model.addAttribute("tipoHabitacion", new TipoHabitacion());
        return "admin/tipos-habitacion-formulario";
    }

    // ===== Mostrar el formulario para editar uno existente =====
    @GetMapping("/{id}/editar")
    public String mostrarFormularioEdicion(@PathVariable Long id, Model model) {
        TipoHabitacion tipoHabitacion = tipoHabitacionService.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Tipo de habitación no encontrado: " + id));
        model.addAttribute("tipoHabitacion", tipoHabitacion);
        return "admin/tipos-habitacion-formulario";
    }

    // ===== Procesar el guardado (sirve tanto para crear como para editar) =====
    @PostMapping("/guardar")
    public String guardar(@ModelAttribute TipoHabitacion tipoHabitacion) {
        tipoHabitacionService.guardar(tipoHabitacion);
        return "redirect:/admin/tipos-habitacion";
    }

    // ===== Eliminar un tipo de habitación =====
    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id) {
        tipoHabitacionService.eliminar(id);
        return "redirect:/admin/tipos-habitacion";
    }
}