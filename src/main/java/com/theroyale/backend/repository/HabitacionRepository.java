package com.theroyale.backend.repository;

import com.theroyale.backend.model.Habitacion;
import com.theroyale.backend.model.TipoHabitacion;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class HabitacionRepository {

    private final List<Habitacion> habitaciones = new ArrayList<>();
    private Long siguienteId = 4L; // arranca en 4 porque ya hay 3 habitaciones de prueba precargadas

    public HabitacionRepository() {
        habitaciones.add(new Habitacion(1L, "101",
                new TipoHabitacion(1L, "Normal", "Elegant essentials for a refined stay. 1-2 guests, double bed, private bathroom, premium Wi-Fi.", "/images/suite-3.webp"),
                120.0, "Disponible"));
        habitaciones.add(new Habitacion(2L, "202",
                new TipoHabitacion(2L, "Executive", "Designed for those who work while they travel. 1-2 guests, executive workspace, king bed, premium Wi-Fi.", "/images/suite-1.webp"),
                190.0, "Ocupada"));
        habitaciones.add(new Habitacion(3L, "301",
                new TipoHabitacion(4L, "Luxury", "The ultimate expression of The Royale. Up to 4 guests, full suite, jacuzzi, privileged city view.", "/images/suite-4.webp"),
                350.0, "Disponible"));
    }

    public List<Habitacion> obtenerTodos() {
        return habitaciones;
    }

    public Optional<Habitacion> obtenerPorId(Long id) {
        return habitaciones.stream()
                .filter(habitacion -> habitacion.getId().equals(id))
                .findFirst();
    }

    // ===== Crea una habitación nueva si no tiene id, o actualiza una existente si ya lo tiene =====
    public Habitacion guardar(Habitacion habitacion) {
        if (habitacion.getId() == null) {
            habitacion.setId(siguienteId);
            siguienteId++;
            habitaciones.add(habitacion);
        } else {
            eliminarPorId(habitacion.getId());
            habitaciones.add(habitacion);
        }
        return habitacion;
    }

    public void eliminarPorId(Long id) {
        habitaciones.removeIf(habitacion -> habitacion.getId().equals(id));
    }
}
