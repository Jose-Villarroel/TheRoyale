package com.theroyale.backend.controller;

import com.theroyale.backend.service.InterfaceService;
import com.theroyale.backend.service.TipoHabitacionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

// ===== Controlador de páginas generales del sitio (landing y páginas estáticas de contenido) =====
@Controller
public class PaginaController {

    private final TipoHabitacionService tipoHabitacionService;
    private final InterfaceService servicioService;

    public PaginaController(TipoHabitacionService tipoHabitacionService, InterfaceService servicioService) {
        this.tipoHabitacionService = tipoHabitacionService;
        this.servicioService = servicioService;
    }


    @GetMapping("/")
    public String mostrarLanding(Model model) {
        model.addAttribute("tiposHabitacion", tipoHabitacionService.listarTodos());
        // Los servicios de la landing salen de la base de datos, los mismos que se cargan a la cuenta
        model.addAttribute("servicios", servicioService.listarServicios());
        return "landing";
    }

    @GetMapping("/admin")
    public String mostrarAdmin() {
        return "redirect:/admin/habitaciones";
    }
}
