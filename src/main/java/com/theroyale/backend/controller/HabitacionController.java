package com.theroyale.backend.controller;

import com.theroyale.backend.model.Habitacion;
import com.theroyale.backend.model.TipoHabitacion;
import com.theroyale.backend.service.HabitacionService;
import com.theroyale.backend.service.TipoHabitacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/habitaciones")
public class HabitacionController {

    @Autowired
    private HabitacionService habitacionService;

    @Autowired
    private TipoHabitacionService tipoHabitacionService;

    // ===== Listar todas las habitaciones =====
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("habitaciones", habitacionService.listarTodos());
        return "admin/habitaciones-lista";
    }

    // ===== Mostrar el formulario para crear una nueva =====
    @GetMapping("/nuevo")
    public String mostrarFormularioCreacion(Model model) {
        model.addAttribute("habitacion", new Habitacion());
        model.addAttribute("tiposHabitacion", tipoHabitacionService.listarTodos());
        return "admin/habitaciones-formulario";
    }

    // ===== Mostrar el formulario para editar una existente =====
    @GetMapping("/{id}/editar")
    public String mostrarFormularioEdicion(@PathVariable Long id, Model model) {
        Habitacion habitacion = habitacionService.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Habitación no encontrada: " + id));
        model.addAttribute("habitacion", habitacion);
        model.addAttribute("tiposHabitacion", tipoHabitacionService.listarTodos());
        return "admin/habitaciones-formulario";
    }

    // ===== Procesar el guardado (sirve tanto para crear como para editar) =====
    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Habitacion habitacion, @RequestParam Long tipoHabitacionId) {
        TipoHabitacion tipoHabitacion = tipoHabitacionService.buscarPorId(tipoHabitacionId)
                .orElseThrow(() -> new IllegalArgumentException("Tipo de habitación no encontrado: " + tipoHabitacionId));
        habitacion.setTipoHabitacion(tipoHabitacion);
        habitacionService.guardar(habitacion);
        return "redirect:/admin/habitaciones";
    }

    // ===== Eliminar una habitación =====
    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id) {
        habitacionService.eliminar(id);
        return "redirect:/admin/habitaciones";
    }
}
