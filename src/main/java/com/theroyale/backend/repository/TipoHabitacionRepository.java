package com.theroyale.backend.repository;

import com.theroyale.backend.model.TipoHabitacion;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class TipoHabitacionRepository {

    private final List<TipoHabitacion> tiposHabitacion = new ArrayList<>();
    private Long siguienteId = 5L; // arranca en 5 porque ya hay 4 tipos de prueba precargados

    public TipoHabitacionRepository() {
        tiposHabitacion.add(new TipoHabitacion(1L, "Normal", "Elegant essentials for a refined stay. 1-2 guests, double bed, private bathroom, premium Wi-Fi.", "/images/suite-3.webp"));
        tiposHabitacion.add(new TipoHabitacion(2L, "Executive", "Designed for those who work while they travel. 1-2 guests, executive workspace, king bed, premium Wi-Fi.", "/images/suite-1.webp"));
        tiposHabitacion.add(new TipoHabitacion(3L, "VIP", "A private space to relax and unwind. Up to 3 guests, separate living area, premium amenities, city views.", "/images/suite-2.webp"));
        tiposHabitacion.add(new TipoHabitacion(4L, "Luxury", "The ultimate expression of The Royale. Up to 4 guests, full suite, jacuzzi, privileged city view.", "/images/suite-4.webp"));
    }

    public List<TipoHabitacion> obtenerTodos() {
        return tiposHabitacion;
    }

    public Optional<TipoHabitacion> obtenerPorId(Long id) {
        return tiposHabitacion.stream()
                .filter(tipo -> tipo.getId().equals(id))
                .findFirst();
    }

    // ===== Crea un tipo nuevo si no tiene id, o actualiza uno existente si ya lo tiene =====
    public TipoHabitacion guardar(TipoHabitacion tipoHabitacion) {
        if (tipoHabitacion.getId() == null) {
            tipoHabitacion.setId(siguienteId);
            siguienteId++;
            tiposHabitacion.add(tipoHabitacion);
        } else {
            eliminarPorId(tipoHabitacion.getId());
            tiposHabitacion.add(tipoHabitacion);
        }
        return tipoHabitacion;
    }

    public void eliminarPorId(Long id) {
        tiposHabitacion.removeIf(tipo -> tipo.getId().equals(id));
    }
}