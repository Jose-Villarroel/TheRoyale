package com.theroyale.backend.service;

import com.theroyale.backend.errors.RecursoNoEncontradoException;
import com.theroyale.backend.model.Admin;
import com.theroyale.backend.model.Servicio;
import com.theroyale.backend.repository.AdminRepository;
import com.theroyale.backend.repository.ItemConsumoRepository;
import com.theroyale.backend.repository.ServicioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// ===== Reglas del catálogo de servicios: nombre único, precio no negativo y borrado seguro =====
// Un servicio ya cargado a alguna cuenta no se puede borrar, para no perder el historial de consumos.
@Service
@Transactional(readOnly = true)
public class ServicioService implements InterfaceService {

    @Autowired
    private ServicioRepository servicioRepository;

    @Autowired
    private ItemConsumoRepository itemConsumoRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Override
    public List<Servicio> listarServicios() {
        return servicioRepository.findAll();
    }

    @Override
    public Servicio buscarPorId(Long id) {
        return servicioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado: " + id));
    }

    @Override
    public Servicio buscarPorNombre(String nombre) {
        return servicioRepository.findByNombreIgnoreCase(nombre)
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado: " + nombre));
    }

    // Al editar se copian solo los campos editables sobre la entidad existente:
    // hacer merge del objeto entrante dejaria sus itemsConsumo fuera del contexto de persistencia.
    @Override
    @Transactional
    public Servicio guardar(Servicio servicio) {
        if (servicio == null) {
            throw new IllegalArgumentException("Service is required.");
        }
        if (servicio.getNombre() == null || servicio.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("Service name is required.");
        }
        if (servicio.getPrecio() < 0) {
            throw new IllegalArgumentException("Price cannot be negative.");
        }

        String nombre = servicio.getNombre().trim();
        Optional<Servicio> conMismoNombre = servicioRepository.findByNombreIgnoreCase(nombre);
        if (conMismoNombre.isPresent() && !conMismoNombre.get().getId().equals(servicio.getId())) {
            throw new IllegalArgumentException("A service with name " + nombre + " already exists.");
        }

        if (servicio.getId() == null) {
            servicio.setNombre(nombre);
            return servicioRepository.save(servicio);
        }

        Servicio existente = buscarPorId(servicio.getId());
        existente.setNombre(nombre);
        existente.setDescripcion(servicio.getDescripcion());
        existente.setPrecio(servicio.getPrecio());
        existente.setImagenUrl(servicio.getImagenUrl());
        // Las @ElementCollection se reemplazan vaciando la lista gestionada, no sustituyendola:
        // asignar una lista nueva confunde a Hibernate con el @OrderColumn.
        reemplazar(existente.getCaracteristicas(), servicio.getCaracteristicas());
        reemplazar(existente.getGaleriaUrls(), servicio.getGaleriaUrls());
        return servicioRepository.save(existente);
    }

    @Override
    @Transactional
    public Servicio crear(Servicio servicio) {
        if (servicio != null) {
            servicio.setId(null);
        }
        return guardar(servicio);
    }

    @Override
    @Transactional
    public Servicio actualizar(Long id, Servicio servicio) {
        if (servicio != null) {
            servicio.setId(id);
        }
        return guardar(servicio);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        buscarPorId(id);
        if (itemConsumoRepository.existsByServicioId(id)) {
            throw new IllegalStateException("This service has registered consumptions and cannot be deleted.");
        }

        for (Admin admin : adminRepository.findByServiciosAdministradosId(id)) {
            admin.getServiciosAdministrados().removeIf(s -> s.getId().equals(id));
        }
        servicioRepository.deleteById(id);
    }

    private void reemplazar(List<String> destino, List<String> origen) {
        destino.clear();
        destino.addAll(origen == null ? new ArrayList<>() : origen);
    }
}
