package com.theroyale.backend.service;

import com.theroyale.backend.errors.RecursoNoEncontradoException;
import com.theroyale.backend.model.TipoHabitacion;
import com.theroyale.backend.repository.HabitacionRepository;
import com.theroyale.backend.repository.TipoHabitacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TipoHabitacionService {

    private final TipoHabitacionRepository tipoHabitacionRepository;
    private final HabitacionRepository habitacionRepository;

    public TipoHabitacionService(TipoHabitacionRepository tipoHabitacionRepository,
                                 HabitacionRepository habitacionRepository) {
        this.tipoHabitacionRepository = tipoHabitacionRepository;
        this.habitacionRepository = habitacionRepository;
    }

    public List<TipoHabitacion> listarTodos() {
        return tipoHabitacionRepository.findAll();
    }

    public TipoHabitacion buscarPorId(Long id) {
        return obtenerPorId(id);
    }

    public TipoHabitacion obtenerPorId(Long id) {
        return tipoHabitacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tipo de habitacion no encontrado: " + id));
    }

    @Transactional
    public TipoHabitacion guardar(TipoHabitacion tipoHabitacion) {
        return tipoHabitacionRepository.save(tipoHabitacion);
    }

    @Transactional
    public void eliminar(Long id) {
        obtenerPorId(id);
        if (habitacionRepository.existsByTipoHabitacionId(id)) {
            throw new IllegalStateException("Ese tipo de habitacion aun tiene habitaciones asociadas.");
        }
        tipoHabitacionRepository.deleteById(id);
    }
}
