package com.theroyale.backend.controller;

import com.theroyale.backend.model.Cliente;
import com.theroyale.backend.service.ClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/clientes")
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("clientes", clienteService.listarTodos());
        return "admin/clientes-lista";
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioCreacion(Model model) {
        model.addAttribute("cliente", new Cliente());
        return "admin/clientes-formulario";
    }

    @GetMapping("/{id}/editar")
    public String mostrarFormularioEdicion(@PathVariable Long id, Model model) {
        model.addAttribute("cliente", clienteService.obtenerPorId(id));
        return "admin/clientes-formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Cliente cliente, Model model) {
        try {
            if (cliente.getId() == null) {
                clienteService.crear(cliente);
            } else {
                clienteService.actualizar(cliente.getId(), cliente);
            }
            return "redirect:/admin/clientes";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("cliente", cliente);
            model.addAttribute("error", ex.getMessage());
            return "admin/clientes-formulario";
        }
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id) {
        clienteService.eliminar(id);
        return "redirect:/admin/clientes";
    }
}
