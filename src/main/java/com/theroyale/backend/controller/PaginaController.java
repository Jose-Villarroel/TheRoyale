package com.theroyale.backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// ===== Controlador de páginas generales del sitio (landing y páginas estáticas de contenido) =====
@Controller
public class PaginaController {

    @GetMapping("/")
    public String mostrarLanding() {
        return "landing";
    }

    @GetMapping("/admin")
    public String mostrarAdmin() {
        return "redirect:/admin/habitaciones";
    }
}
