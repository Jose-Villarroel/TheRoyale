package com.theroyale.backend.service;

import com.theroyale.backend.errors.RecursoNoEncontradoException;
import com.theroyale.backend.model.Servicio;
import com.theroyale.backend.repository.ServicioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ServicioService implements InterfaceService {

    private final ServicioRepository servicioRepository;

    public ServicioService(ServicioRepository servicioRepository) {
        this.servicioRepository = servicioRepository;
    }

    @Override
    public List<Servicio> listarServicios() {
        return servicioRepository.findAll();
    }

    @Override
    public Optional<Servicio> buscarPorId(Long id) {
        return servicioRepository.findById(id);
    }

    @Override
    public Optional<Servicio> buscarPorNombre(String nombre) {
        Optional<Servicio> servicio = servicioRepository.findByNombreIgnoreCase(nombre);

        if (servicio.isEmpty()) {
            throw new RecursoNoEncontradoException("Servicio no encontrado: " + nombre);
        }

        return servicio;
    }
}
