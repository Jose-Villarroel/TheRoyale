package com.theroyale.backend.service;

import com.theroyale.backend.model.TipoHabitacion;
import com.theroyale.backend.repository.TipoHabitacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TipoHabitacionService {

    @Autowired
    private TipoHabitacionRepository tipoHabitacionRepository;

    public List<TipoHabitacion> listarTodos() {
        return tipoHabitacionRepository.obtenerTodos();
    }

    public Optional<TipoHabitacion> buscarPorId(Long id) {
        return tipoHabitacionRepository.obtenerPorId(id);
    }

    public TipoHabitacion guardar(TipoHabitacion tipoHabitacion) {
        return tipoHabitacionRepository.guardar(tipoHabitacion);
    }

    public void eliminar(Long id) {
        tipoHabitacionRepository.eliminarPorId(id);
    }
}
