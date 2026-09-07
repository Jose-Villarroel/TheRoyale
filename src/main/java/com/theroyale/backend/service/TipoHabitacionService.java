package com.theroyale.backend.service;

import com.theroyale.backend.model.TipoHabitacion;
import com.theroyale.backend.repository.TipoHabitacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

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
        return tipoHabitacionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Tipo de habitación no encontrado: " + id));
    }

    @Transactional
    public TipoHabitacion guardar(TipoHabitacion tipoHabitacion) {
        return tipoHabitacionRepository.save(tipoHabitacion);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!tipoHabitacionRepository.existsById(id)) {
            throw new NoSuchElementException("Tipo de habitación no encontrado: " + id);
        }
        tipoHabitacionRepository.deleteById(id);
    }
}
