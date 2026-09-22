package com.theroyale.backend.service;

import com.theroyale.backend.errors.RecursoNoEncontradoException;
import com.theroyale.backend.model.Admin;
import com.theroyale.backend.model.EstadoHabitacion;
import com.theroyale.backend.model.Habitacion;
import com.theroyale.backend.repository.AdminRepository;
import com.theroyale.backend.repository.HabitacionRepository;
import com.theroyale.backend.repository.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class HabitacionService {

    private final HabitacionRepository habitacionRepository;
    private final ReservaRepository reservaRepository;
    private final AdminRepository adminRepository;

    public HabitacionService(HabitacionRepository habitacionRepository,
                             ReservaRepository reservaRepository,
                             AdminRepository adminRepository) {
        this.habitacionRepository = habitacionRepository;
        this.reservaRepository = reservaRepository;
        this.adminRepository = adminRepository;
    }

    public List<Habitacion> listarTodos() {
        return habitacionRepository.findAll();
    }

    public List<Habitacion> listarOrdenadas() {
        return habitacionRepository.findAllByOrderByNumeroAsc();
    }

    public List<Habitacion> listarPorTipoHabitacion(Long tipoHabitacionId) {
        return habitacionRepository.findByTipoHabitacionIdOrderByNumeroAsc(tipoHabitacionId);
    }

    public long contarPorEstado(EstadoHabitacion estado) {
        return habitacionRepository.countByEstado(estado);
    }

    public Habitacion buscarPorId(Long id) {
        return obtenerPorId(id);
    }

    public Habitacion obtenerPorId(Long id) {
        return habitacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Habitacion no encontrada: " + id));
    }

    // Al editar se copian solo los campos del formulario sobre la entidad existente,
    // para no reemplazar sus relaciones (reservas) con las del objeto enlazado.
    @Transactional
    public Habitacion guardar(Habitacion habitacion) {
        if (habitacion == null) {
            throw new IllegalArgumentException("Room is required.");
        }
        if (habitacion.getNumero() == null || habitacion.getNumero().trim().isEmpty()) {
            throw new IllegalArgumentException("Room number is required.");
        }
        if (habitacion.getTipoHabitacion() == null) {
            throw new IllegalArgumentException("Room type is required.");
        }
        if (habitacion.getEstado() == null) {
            throw new IllegalArgumentException("Room status is required.");
        }
        if (habitacion.getPrecio() < 0) {
            throw new IllegalArgumentException("Price cannot be negative.");
        }

        String numero = habitacion.getNumero().trim();
        Optional<Habitacion> conMismoNumero = habitacionRepository.findByNumero(numero);
        if (conMismoNumero.isPresent() && !conMismoNumero.get().getId().equals(habitacion.getId())) {
            throw new IllegalArgumentException("A room with number " + numero + " already exists.");
        }

        if (habitacion.getId() == null) {
            habitacion.setNumero(numero);
            return habitacionRepository.save(habitacion);
        }

        Habitacion existente = obtenerPorId(habitacion.getId());
        existente.setNumero(numero);
        existente.setTipoHabitacion(habitacion.getTipoHabitacion());
        existente.setPrecio(habitacion.getPrecio());
        existente.setEstado(habitacion.getEstado());
        return habitacionRepository.save(existente);
    }

    @Transactional
    public Habitacion cambiarEstado(Long id, EstadoHabitacion estado) {
        if (estado == null) {
            throw new IllegalArgumentException("Room status is required.");
        }
        Habitacion habitacion = obtenerPorId(id);
        habitacion.setEstado(estado);
        return habitacionRepository.save(habitacion);
    }

    @Transactional
    public void eliminar(Long id) {
        Habitacion habitacion = obtenerPorId(id);
        if (reservaRepository.existsByHabitacionId(id)) {
            throw new IllegalStateException("Esta habitacion tiene reservas asociadas y no puede ser eliminada.");
        }

        // Los admins son el lado dueno de la relacion: hay que quitarla antes de borrar
        for (Admin admin : adminRepository.findByHabitacionesAdministradasId(id)) {
            admin.getHabitacionesAdministradas().removeIf(h -> h.getId().equals(id));
        }
        habitacionRepository.deleteById(id);
    }
}
