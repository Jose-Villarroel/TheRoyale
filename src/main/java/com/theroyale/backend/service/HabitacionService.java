package com.theroyale.backend.service;

import com.theroyale.backend.model.Habitacion;
import com.theroyale.backend.repository.HabitacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional(readOnly = true)
public class HabitacionService {

    private final HabitacionRepository habitacionRepository;

    public HabitacionService(HabitacionRepository habitacionRepository) {
        this.habitacionRepository = habitacionRepository;
    }

    public List<Habitacion> listarTodos() {
        return habitacionRepository.findAll();
    }

    public Habitacion buscarPorId(Long id) {
        return obtenerPorId(id);
    }

    public Habitacion obtenerPorId(Long id) {
        return habitacionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Habitacion no encontrada: " + id));
    }

    @Transactional
    public Habitacion guardar(Habitacion habitacion) {
        return habitacionRepository.save(habitacion);
    }

    @Transactional
    public void eliminar(Long id) {
        obtenerPorId(id);
        habitacionRepository.deleteById(id);
    }
}
