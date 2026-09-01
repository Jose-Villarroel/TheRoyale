package com.theroyale.backend.controller;

import com.theroyale.backend.model.Cliente;
import com.theroyale.backend.service.AutenticacionService;
import com.theroyale.backend.service.ClienteService;
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
    private final ClienteService clienteService;

    public AutenticacionController(AutenticacionService autenticacionService, ClienteService clienteService) {
        this.autenticacionService = autenticacionService;
        this.clienteService = clienteService;
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
        Cliente cliente = obtenerClienteAutenticado(session);

        if (cliente == null) {
            return "redirect:/login";
        }

        model.addAttribute("cliente", cliente);
        return "reservations";
    }

    @GetMapping("/profile")
    public String mostrarPerfil(HttpSession session, Model model) {
        Cliente cliente = obtenerClienteAutenticado(session);

        if (cliente == null) {
            return "redirect:/login";
        }

        if (!model.containsAttribute("cliente")) {
            model.addAttribute("cliente", cliente);
        }
        return "profile";
    }

    @PostMapping("/profile")
    public String actualizarPerfil(@ModelAttribute Cliente clienteActualizado,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        Cliente cliente = obtenerClienteAutenticado(session);

        if (cliente == null) {
            return "redirect:/login";
        }

        try {
            Cliente clienteGuardado = clienteService.actualizar(cliente.getId(), clienteActualizado);
            session.setAttribute("clienteAutenticado", clienteGuardado);
            redirectAttributes.addFlashAttribute("mensaje", "Profile updated successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            redirectAttributes.addFlashAttribute("cliente", clienteActualizado);
        }

        return "redirect:/profile";
    }

    @PostMapping("/profile/delete")
    public String eliminarPerfil(HttpSession session, RedirectAttributes redirectAttributes) {
        Cliente cliente = obtenerClienteAutenticado(session);

        if (cliente == null) {
            return "redirect:/login";
        }

        clienteService.eliminar(cliente.getId());
        session.invalidate();
        redirectAttributes.addFlashAttribute("mensaje", "Your profile was deleted.");
        return "redirect:/signup";
    }

    @PostMapping("/logout")
    public String cerrarSesion(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    private Cliente obtenerClienteAutenticado(HttpSession session) {
        Cliente clienteSesion = (Cliente) session.getAttribute("clienteAutenticado");

        if (clienteSesion == null || clienteSesion.getId() == null) {
            return null;
        }

        return clienteService.buscarPorId(clienteSesion.getId())
                .map(cliente -> {
                    session.setAttribute("clienteAutenticado", cliente);
                    return cliente;
                })
                .orElseGet(() -> {
                    session.invalidate();
                    return null;
                });
    }

    private boolean estaVacio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }
}
