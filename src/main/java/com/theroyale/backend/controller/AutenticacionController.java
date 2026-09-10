package com.theroyale.backend.controller;

import com.theroyale.backend.model.Cliente;
import com.theroyale.backend.service.AutenticacionService;
import com.theroyale.backend.service.AutenticacionService.ResultadoRegistro;
import com.theroyale.backend.service.ClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class AutenticacionController {

    @Autowired
    private AutenticacionService autenticacionService;

    @Autowired
    private ClienteService clienteService;

    @GetMapping("/login")
    public String mostrarLogin(@RequestParam(required = false) String email,
                               @RequestParam(required = false) String error,
                               @RequestParam(required = false) String mensaje,
                               Model model) {
        model.addAttribute("email", email == null ? "" : email);

        if ("invalid".equals(error)) {
            model.addAttribute("error", "Email or password is incorrect.");
        }

        if ("accountCreated".equals(mensaje)) {
            model.addAttribute("mensaje", "Account created. Please sign in.");
        }

        return "sign-in";
    }

    @PostMapping("/login")
    public String iniciarSesion(String email, String password, RedirectAttributes redirectAttributes) {
        Optional<Cliente> clienteAutenticado = autenticacionService.autenticar(email, password);

        if (clienteAutenticado.isEmpty()) {
            redirectAttributes.addAttribute("error", "invalid");
            redirectAttributes.addAttribute("email", email);
            return "redirect:/login";
        }

        return "redirect:/reservations?clienteId=" + clienteAutenticado.get().getId();
    }

    @GetMapping("/signup")
    public String mostrarRegistro(@RequestParam(required = false) String error,
                                  @RequestParam(required = false) String mensaje,
                                  Model model) {
        model.addAttribute("cliente", new Cliente());

        if ("incomplete".equals(error)) {
            model.addAttribute("error", "Please complete all fields.");
        } else if ("passwordMismatch".equals(error)) {
            model.addAttribute("error", "Passwords do not match.");
        } else if ("duplicate".equals(error)) {
            model.addAttribute("error", "An account with this email already exists.");
        }

        if ("profileDeleted".equals(mensaje)) {
            model.addAttribute("mensaje", "Your profile was deleted.");
        }

        return "sign-up";
    }

    @PostMapping("/signup")
    public String registrar(@ModelAttribute Cliente cliente, String confirmPassword, RedirectAttributes redirectAttributes) {
        ResultadoRegistro resultado = autenticacionService.registrarCliente(cliente, confirmPassword);

        if (resultado == ResultadoRegistro.INCOMPLETO) {
            redirectAttributes.addAttribute("error", "incomplete");
            return "redirect:/signup";
        }

        if (resultado == ResultadoRegistro.PASSWORD_NO_COINCIDE) {
            redirectAttributes.addAttribute("error", "passwordMismatch");
            return "redirect:/signup";
        }

        if (resultado == ResultadoRegistro.DUPLICADO) {
            redirectAttributes.addAttribute("error", "duplicate");
            return "redirect:/signup";
        }

        redirectAttributes.addAttribute("mensaje", "accountCreated");
        redirectAttributes.addAttribute("email", cliente.getEmail());
        return "redirect:/login";
    }

    @GetMapping("/reservations")
    public String mostrarReservas(@RequestParam(required = false) Long clienteId, Model model) {
        Optional<Cliente> cliente = autenticacionService.obtenerClienteAutenticado(clienteId);

        if (cliente.isEmpty()) {
            return "redirect:/login";
        }

        model.addAttribute("cliente", cliente.get());
        return "reservations";
    }

    @GetMapping("/profile")
    public String mostrarPerfil(@RequestParam(required = false) Long clienteId,
                                @RequestParam(required = false) String mensaje,
                                Model model) {
        Optional<Cliente> cliente = autenticacionService.obtenerClienteAutenticado(clienteId);

        if (cliente.isEmpty()) {
            return "redirect:/login";
        }

        if (!model.containsAttribute("cliente")) {
            model.addAttribute("cliente", cliente.get());
        }

        if ("profileUpdated".equals(mensaje)) {
            model.addAttribute("mensaje", "Profile updated successfully.");
        }

        return "profile";
    }

    @PostMapping("/profile")
    public String actualizarPerfil(@ModelAttribute Cliente clienteActualizado,
                                   @RequestParam(required = false) Long clienteId,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        Optional<Cliente> clienteExistente = autenticacionService.obtenerClienteAutenticado(clienteId);

        if (clienteExistente.isEmpty()) {
            return "redirect:/login";
        }

        try {
            Cliente clienteGuardado = clienteService.actualizar(clienteId, clienteActualizado);
            redirectAttributes.addAttribute("clienteId", clienteGuardado.getId());
            redirectAttributes.addAttribute("mensaje", "profileUpdated");
            return "redirect:/profile";
        } catch (IllegalArgumentException ex) {
            clienteActualizado.setId(clienteId);
            clienteActualizado.setFechaRegistro(clienteExistente.get().getFechaRegistro());
            model.addAttribute("cliente", clienteActualizado);
            model.addAttribute("error", ex.getMessage());
            return "profile";
        }
    }

    @PostMapping("/profile/delete")
    public String eliminarPerfil(@RequestParam(required = false) Long clienteId, RedirectAttributes redirectAttributes) {
        if (autenticacionService.obtenerClienteAutenticado(clienteId).isEmpty()) {
            return "redirect:/login";
        }

        clienteService.eliminar(clienteId);
        redirectAttributes.addAttribute("mensaje", "profileDeleted");
        return "redirect:/signup";
    }
}
