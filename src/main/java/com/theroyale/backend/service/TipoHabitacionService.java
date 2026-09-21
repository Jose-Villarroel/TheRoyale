package com.theroyale.backend.service;

import com.theroyale.backend.errors.RecursoNoEncontradoException;
import com.theroyale.backend.model.Admin;
import com.theroyale.backend.model.TipoHabitacion;
import com.theroyale.backend.repository.AdminRepository;
import com.theroyale.backend.repository.HabitacionRepository;
import com.theroyale.backend.repository.TipoHabitacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class TipoHabitacionService {

    private final TipoHabitacionRepository tipoHabitacionRepository;
    private final HabitacionRepository habitacionRepository;
    private final AdminRepository adminRepository;

    public TipoHabitacionService(TipoHabitacionRepository tipoHabitacionRepository,
                                 HabitacionRepository habitacionRepository,
                                 AdminRepository adminRepository) {
        this.tipoHabitacionRepository = tipoHabitacionRepository;
        this.habitacionRepository = habitacionRepository;
        this.adminRepository = adminRepository;
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

    // Al editar se copian solo los campos del formulario sobre la entidad existente:
    // hacer merge del objeto enlazado dejaria la galeria de imagenes vacia y orphanRemoval la borraria.
    @Transactional
    public TipoHabitacion guardar(TipoHabitacion tipoHabitacion) {
        if (tipoHabitacion == null) {
            throw new IllegalArgumentException("Room type is required.");
        }
        if (tipoHabitacion.getNombre() == null || tipoHabitacion.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("Room type name is required.");
        }

        String nombre = tipoHabitacion.getNombre().trim();
        Optional<TipoHabitacion> conMismoNombre = tipoHabitacionRepository.findByNombreIgnoreCase(nombre);
        if (conMismoNombre.isPresent() && !conMismoNombre.get().getId().equals(tipoHabitacion.getId())) {
            throw new IllegalArgumentException("A room type with name " + nombre + " already exists.");
        }

        if (tipoHabitacion.getId() == null) {
            tipoHabitacion.setNombre(nombre);
            return tipoHabitacionRepository.save(tipoHabitacion);
        }

        TipoHabitacion existente = obtenerPorId(tipoHabitacion.getId());
        existente.setNombre(nombre);
        existente.setDescripcion(tipoHabitacion.getDescripcion());
        existente.setImagenUrl(tipoHabitacion.getImagenUrl());
        return tipoHabitacionRepository.save(existente);
    }

    @Transactional
    public void eliminar(Long id) {
        obtenerPorId(id);
        if (habitacionRepository.existsByTipoHabitacionId(id)) {
            throw new IllegalStateException("This room type still has associated rooms and cannot be deleted.");
        }

        for (Admin admin : adminRepository.findByTiposHabitacionAdministradosId(id)) {
            admin.getTiposHabitacionAdministrados().removeIf(t -> t.getId().equals(id));
        }
        // Las imagenes de la galeria se borran en cascada (CascadeType.ALL)
        tipoHabitacionRepository.deleteById(id);
    }
}
