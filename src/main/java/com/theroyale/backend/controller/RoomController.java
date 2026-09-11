package com.theroyale.backend.controller;

import com.theroyale.backend.model.EstadoHabitacion;
import com.theroyale.backend.model.Habitacion;
import com.theroyale.backend.model.TipoHabitacion;
import com.theroyale.backend.service.HabitacionService;
import com.theroyale.backend.service.TipoHabitacionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class RoomController {

    private final TipoHabitacionService tipoHabitacionService;
    private final HabitacionService habitacionService;

    public RoomController(TipoHabitacionService tipoHabitacionService, HabitacionService habitacionService) {
        this.tipoHabitacionService = tipoHabitacionService;
        this.habitacionService = habitacionService;
    }

    @GetMapping("/rooms")
    public String listarRooms(Model model) {
        List<TipoHabitacion> tiposHabitacion = tipoHabitacionService.listarTodos();
        List<Habitacion> habitaciones = habitacionService.listarTodos();

        Map<Long, Double> preciosDesdePorTipo = habitaciones.stream()
                .collect(Collectors.groupingBy(
                        habitacion -> habitacion.getTipoHabitacion().getId(),
                        Collectors.collectingAndThen(
                                Collectors.mapping(Habitacion::getPrecio, Collectors.minBy(Comparator.naturalOrder())),
                                precio -> precio.orElse(null)
                        )
                ));

        Map<Long, Long> habitacionesDisponiblesPorTipo = habitaciones.stream()
                .filter(habitacion -> EstadoHabitacion.DISPONIBLE.equals(habitacion.getEstado()))
                .collect(Collectors.groupingBy(
                        habitacion -> habitacion.getTipoHabitacion().getId(),
                        Collectors.counting()
                ));

        model.addAttribute("tiposHabitacion", tiposHabitacion);
        model.addAttribute("preciosDesdePorTipo", preciosDesdePorTipo);
        model.addAttribute("habitacionesDisponiblesPorTipo", habitacionesDisponiblesPorTipo);
        return "rooms-lista";
    }

    @GetMapping("/rooms/{id}")
    public String mostrarDetalleRoom(@PathVariable Long id, Model model) {
        TipoHabitacion tipoHabitacion = tipoHabitacionService.obtenerPorId(id);
        List<Habitacion> habitaciones = habitacionService.listarPorTipoHabitacion(id);

        Optional<Double> precioDesde = habitaciones.stream()
                .map(Habitacion::getPrecio)
                .min(Comparator.naturalOrder());

        long habitacionesDisponibles = habitaciones.stream()
                .filter(habitacion -> EstadoHabitacion.DISPONIBLE.equals(habitacion.getEstado()))
                .count();

        model.addAttribute("tipoHabitacion", tipoHabitacion);
        model.addAttribute("habitaciones", habitaciones);
        model.addAttribute("precioDesde", precioDesde);
        model.addAttribute("habitacionesDisponibles", habitacionesDisponibles);
        return "room-detalle";
    }
}
