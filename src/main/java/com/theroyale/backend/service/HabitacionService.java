package com.theroyale.backend.service;

import com.theroyale.backend.model.Habitacion;
import com.theroyale.backend.repository.HabitacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class HabitacionService {

    @Autowired
    private HabitacionRepository habitacionRepository;

    public List<Habitacion> listarTodos() {
        return habitacionRepository.obtenerTodos();
    }

    public Optional<Habitacion> buscarPorId(Long id) {
        return habitacionRepository.obtenerPorId(id);
    }

    public Habitacion guardar(Habitacion habitacion) {
        return habitacionRepository.guardar(habitacion);
    }

    public void eliminar(Long id) {
        habitacionRepository.eliminarPorId(id);
    }
}
