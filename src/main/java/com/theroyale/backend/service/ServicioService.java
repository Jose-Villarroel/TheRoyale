package com.theroyale.backend.service;

import com.theroyale.backend.model.Servicio;
import com.theroyale.backend.repository.ServicioRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.Optional;

@Service
public class ServicioService implements InterfazService {

    @Autowired
    private ServicioRepository servicioRepository;



    @Override
    public List<Servicio> listarServicios() {
        return servicioRepository.obtenerTodos();
    }

    @Override
    public Optional<Servicio> buscarPorId(Long id) {
        return servicioRepository.obtenerPorId(id);
    }

    @Override
    public Optional<Servicio> buscarPorNombre(String nombre) {
        return servicioRepository.obtenerPorNombre(nombre);
    }
}