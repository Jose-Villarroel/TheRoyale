package com.theroyale.backend.service;

import com.theroyale.backend.model.Servicio;
import com.theroyale.backend.repository.ServicioRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ServicioService {

    private final ServicioRepository servicioRepository;

    
    public ServicioService(ServicioRepository servicioRepository) {
        this.servicioRepository = servicioRepository;
    }

    public List<Servicio> listarServicios() {
        return servicioRepository.obtenerTodos();
    }

    public Optional<Servicio> buscarPorId(Long id) {
        return servicioRepository.obtenerPorId(id);
    }

    public Optional<Servicio> buscarPorNombre(String nombre) {
        return servicioRepository.obtenerPorNombre(nombre);
    }
}