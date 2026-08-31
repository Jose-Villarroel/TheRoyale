package com.theroyale.backend.controller;

import com.theroyale.backend.model.Usuario;
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
        Optional<Usuario> usuarioAutenticado = autenticacionService.autenticar(email, password);

        if (usuarioAutenticado.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Email or password is incorrect.");
            redirectAttributes.addFlashAttribute("email", email);
            return "redirect:/login";
        }

        session.setAttribute("usuarioAutenticado", usuarioAutenticado.get());
        return "redirect:/reservations";
    }

    @GetMapping("/signup")
    public String mostrarRegistro(Model model) {
        if (!model.containsAttribute("usuario")) {
            model.addAttribute("usuario", new Usuario());
        }
        return "sign-up";
    }

    @PostMapping("/signup")
    public String registrar(@ModelAttribute Usuario usuario, String confirmPassword, RedirectAttributes redirectAttributes) {
        if (estaVacio(usuario.getNombre()) || estaVacio(usuario.getApellido()) || estaVacio(usuario.getEmail()) || estaVacio(usuario.getPassword())) {
            redirectAttributes.addFlashAttribute("error", "Please complete all fields.");
            redirectAttributes.addFlashAttribute("usuario", usuario);
            return "redirect:/signup";
        }

        if (!usuario.getPassword().equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match.");
            redirectAttributes.addFlashAttribute("usuario", usuario);
            return "redirect:/signup";
        }

        if (!autenticacionService.registrarUsuario(usuario)) {
            redirectAttributes.addFlashAttribute("error", "An account with this email already exists.");
            redirectAttributes.addFlashAttribute("usuario", usuario);
            return "redirect:/signup";
        }

        redirectAttributes.addFlashAttribute("mensaje", "Account created. Please sign in.");
        redirectAttributes.addFlashAttribute("email", usuario.getEmail());
        return "redirect:/login";
    }

    @GetMapping("/reservations")
    public String mostrarReservas(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioAutenticado");

        if (usuario == null) {
            return "redirect:/login";
        }

        model.addAttribute("usuario", usuario);
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
