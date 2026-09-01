package com.theroyale.backend.controller;

import com.theroyale.backend.model.Cliente;
import com.theroyale.backend.service.AutenticacionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class AutenticacionController {

    private final AutenticacionService autenticacionService;

    public AutenticacionController(AutenticacionService autenticacionService) {
        this.autenticacionService = autenticacionService;
    }

    @GetMapping("/login")
    public String mostrarLogin(Model model) {
        if (!model.containsAttribute("email")) {
            model.addAttribute("email", "");
        }
        return "sign-in";
    }

    @PostMapping("/login")
    public String iniciarSesion(String email, String password, HttpSession session, RedirectAttributes redirectAttributes) {
        Optional<Cliente> clienteAutenticado = autenticacionService.autenticar(email, password);

        if (clienteAutenticado.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Email or password is incorrect.");
            redirectAttributes.addFlashAttribute("email", email);
            return "redirect:/login";
        }

        session.setAttribute("clienteAutenticado", clienteAutenticado.get());
        return "redirect:/reservations";
    }

    @GetMapping("/signup")
    public String mostrarRegistro(Model model) {
        if (!model.containsAttribute("cliente")) {
            model.addAttribute("cliente", new Cliente());
        }
        return "sign-up";
    }

    @PostMapping("/signup")
    public String registrar(@ModelAttribute Cliente cliente, String confirmPassword, RedirectAttributes redirectAttributes) {
        if (estaVacio(cliente.getNombre()) || estaVacio(cliente.getApellido()) || estaVacio(cliente.getEmail()) || estaVacio(cliente.getPassword())) {
            redirectAttributes.addFlashAttribute("error", "Please complete all fields.");
            redirectAttributes.addFlashAttribute("cliente", cliente);
            return "redirect:/signup";
        }

        if (!cliente.getPassword().equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match.");
            redirectAttributes.addFlashAttribute("cliente", cliente);
            return "redirect:/signup";
        }

        if (!autenticacionService.registrarCliente(cliente)) {
            redirectAttributes.addFlashAttribute("error", "An account with this email already exists.");
            redirectAttributes.addFlashAttribute("cliente", cliente);
            return "redirect:/signup";
        }

        redirectAttributes.addFlashAttribute("mensaje", "Account created. Please sign in.");
        redirectAttributes.addFlashAttribute("email", cliente.getEmail());
        return "redirect:/login";
    }

    @GetMapping("/reservations")
    public String mostrarReservas(HttpSession session, Model model) {
        Cliente cliente = (Cliente) session.getAttribute("clienteAutenticado");

        if (cliente == null) {
            return "redirect:/login";
        }

        model.addAttribute("cliente", cliente);
        return "reservations";
    }

    @PostMapping("/logout")
    public String cerrarSesion(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    private boolean estaVacio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }
}
