package com.theroyale.backend.errors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

// ===== Red de seguridad: los controllers capturan los errores de negocio esperados para
// mostrarlos en su propio formulario; lo que se escape termina aquí en la página de error. =====
@ControllerAdvice
public class GlobalExceptionHandler {

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public String manejarRecursoNoEncontrado(RecursoNoEncontradoException ex, Model model) {
        return mostrarError("Not found", ex.getMessage(), model);
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public String manejarIntegridadDatos(DataIntegrityViolationException ex, Model model) {
        return mostrarError("Action not allowed",
                "No se pudo completar la operacion: hay un valor duplicado o el registro esta siendo usado por otros datos.",
                model);
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(IllegalStateException.class)
    public String manejarEstadoInvalido(IllegalStateException ex, Model model) {
        return mostrarError("Action not allowed", ex.getMessage(), model);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public String manejarArgumentoInvalido(IllegalArgumentException ex, Model model) {
        return mostrarError("Invalid request", ex.getMessage(), model);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public String manejarParametrosInvalidos(Exception ex, Model model) {
        return mostrarError("Invalid request", "Faltan datos o alguno de los valores enviados no es valido.", model);
    }

    private String mostrarError(String titulo, String mensaje, Model model) {
        model.addAttribute("titulo", titulo);
        model.addAttribute("mensaje", mensaje);
        return "error";
    }
}
