package com.theroyale.backend.controller;

import com.theroyale.backend.service.TipoHabitacionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

// ===== Controlador de páginas generales del sitio (landing y páginas estáticas de contenido) =====
@Controller
public class PaginaController {

    private final TipoHabitacionService tipoHabitacionService;

    public PaginaController(TipoHabitacionService tipoHabitacionService) {
        this.tipoHabitacionService = tipoHabitacionService;
    }


    @GetMapping("/")
    public String mostrarLanding(Model model) {
        model.addAttribute("tiposHabitacion", tipoHabitacionService.listarTodos());
        return "landing";
    }

    @GetMapping("/admin")
    public String mostrarAdmin() {
        return "redirect:/admin/habitaciones";
    }
}
