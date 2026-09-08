package com.theroyale.backend.service;

import com.theroyale.backend.errors.RecursoNoEncontradoException;
import com.theroyale.backend.model.TipoHabitacion;
import com.theroyale.backend.repository.TipoHabitacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TipoHabitacionService {

    private final TipoHabitacionRepository tipoHabitacionRepository;

    public TipoHabitacionService(TipoHabitacionRepository tipoHabitacionRepository) {
        this.tipoHabitacionRepository = tipoHabitacionRepository;
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
        tipoHabitacionRepository.deleteById(id);
    }
}
