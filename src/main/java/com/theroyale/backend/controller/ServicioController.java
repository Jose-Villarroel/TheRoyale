package com.theroyale.backend.controller;

import com.theroyale.backend.model.Servicio;
import com.theroyale.backend.service.ServicioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

@Controller
public class ServicioController {

    @Autowired
    private ServicioService servicioService;

    // ===== Página con la vista general de todos los servicios (diseño editorial) =====
    @GetMapping("/servicios")
    public String mostrarServicios() {
        return "servicios";
    }

    // ===== Detalle de un único servicio =====
    @GetMapping("/servicios/{id}")
    public String mostrarDetalle(@PathVariable Long id, Model model) {
        Optional<Servicio> servicioEncontrado = servicioService.buscarPorId(id);

        if (servicioEncontrado.isEmpty()) {
            return "servicio-no-encontrado";
        }

        model.addAttribute("servicio", servicioEncontrado.get());
        return "servicio-detalle";
    }
}