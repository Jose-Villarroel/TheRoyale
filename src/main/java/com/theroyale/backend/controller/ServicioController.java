package com.theroyale.backend.controller;

import com.theroyale.backend.model.Servicio;
import com.theroyale.backend.service.ServicioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

@Controller
public class ServicioController {

    private final ServicioService servicioService;

    public ServicioController(ServicioService servicioService) {
        this.servicioService = servicioService;
    }

    @GetMapping("/services/table")
    public String mostrarTabla(Model model) {
        model.addAttribute("servicios", servicioService.listarServicios());
        return "servicios-tabla";
    }

    @GetMapping("/services/cards")
    public String mostrarTarjetas(Model model) {
        model.addAttribute("servicios", servicioService.listarServicios());
        return "servicios-tarjetas";
    }

    @GetMapping("/services/{nombre}")
    public String mostrarDetalle(@PathVariable String nombre, Model model) {
        Optional<Servicio> servicioEncontrado = servicioService.buscarPorNombre(nombre);

        if (servicioEncontrado.isEmpty()) {
            return "servicio-no-encontrado";
        }

        model.addAttribute("servicio", servicioEncontrado.get());
        return "servicio-detalle";
    }
}