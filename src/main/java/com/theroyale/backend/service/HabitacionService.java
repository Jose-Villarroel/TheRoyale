package com.theroyale.backend.service;

import com.theroyale.backend.dto.HabitacionRequest;
import com.theroyale.backend.errors.RecursoNoEncontradoException;
import com.theroyale.backend.model.Admin;
import com.theroyale.backend.model.EstadoHabitacion;
import com.theroyale.backend.model.Habitacion;
import com.theroyale.backend.repository.AdminRepository;
import com.theroyale.backend.repository.HabitacionRepository;
import com.theroyale.backend.repository.ReservaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

// ===== Reglas de las habitaciones físicas: CRUD, estado operativo y resúmenes por tipo =====
// Los resúmenes (precio "desde" y cuántas quedan disponibles) viven aquí y no en los
// controladores para que la vista Thymeleaf y cualquier endpoint futuro calculen lo mismo.
@Service
@Transactional(readOnly = true)
public class HabitacionService {

    @Autowired
    private HabitacionRepository habitacionRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private TipoHabitacionService tipoHabitacionService;

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

    // ===== Resúmenes por tipo de habitación (listado y detalle de /rooms) =====
    // Precio más bajo de cada tipo, sin importar el estado de la habitación
    public Map<Long, Double> preciosDesdePorTipo() {
        return habitacionRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        habitacion -> habitacion.getTipoHabitacion().getId(),
                        Collectors.collectingAndThen(
                                Collectors.mapping(Habitacion::getPrecio, Collectors.minBy(Comparator.naturalOrder())),
                                precio -> precio.orElse(null)
                        )
                ));
    }

    // Habitaciones en estado DISPONIBLE de cada tipo; los tipos sin ninguna no aparecen en el mapa
    public Map<Long, Long> disponiblesPorTipo() {
        return habitacionRepository.findAll().stream()
                .filter(habitacion -> EstadoHabitacion.DISPONIBLE.equals(habitacion.getEstado()))
                .collect(Collectors.groupingBy(
                        habitacion -> habitacion.getTipoHabitacion().getId(),
                        Collectors.counting()
                ));
    }

    public Optional<Double> precioDesde(Long tipoHabitacionId) {
        return listarPorTipoHabitacion(tipoHabitacionId).stream()
                .map(Habitacion::getPrecio)
                .min(Comparator.naturalOrder());
    }

    public long contarDisponibles(Long tipoHabitacionId) {
        return listarPorTipoHabitacion(tipoHabitacionId).stream()
                .filter(habitacion -> EstadoHabitacion.DISPONIBLE.equals(habitacion.getEstado()))
                .count();
    }

    public Habitacion buscarPorId(Long id) {
        return obtenerPorId(id);
    }

    public Habitacion obtenerPorId(Long id) {
        return habitacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Habitacion no encontrada: " + id));
    }

    // ===== Alta y edición desde la API REST: el tipo llega como id y se resuelve aquí =====
    @Transactional
    public Habitacion crear(HabitacionRequest solicitud) {
        Habitacion habitacion = desdeSolicitud(solicitud);
        // El id lo asigna la base: un POST nunca debe sobreescribir una habitación existente
        habitacion.setId(null);
        return guardar(habitacion);
    }

    @Transactional
    public Habitacion actualizar(Long id, HabitacionRequest solicitud) {
        Habitacion habitacion = desdeSolicitud(solicitud);
        habitacion.setId(id);
        return guardar(habitacion);
    }

    // ===== Alta y edición desde el formulario Thymeleaf, que manda el tipo como parámetro aparte =====
    // Se deja el tipo resuelto en el objeto recibido para que, si falla la validación,
    // el formulario se vuelva a pintar con el tipo seleccionado.
    @Transactional
    public Habitacion guardar(Habitacion habitacion, Long tipoHabitacionId) {
        if (habitacion != null && tipoHabitacionId != null) {
            habitacion.setTipoHabitacion(tipoHabitacionService.obtenerPorId(tipoHabitacionId));
        }
        return guardar(habitacion);
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

    // Convierte el DTO en entidad leyendo el tipo de la base, para que Hibernate lo gestione
    private Habitacion desdeSolicitud(HabitacionRequest solicitud) {
        if (solicitud == null) {
            throw new IllegalArgumentException("Room is required.");
        }
        if (solicitud.tipoHabitacionId() == null) {
            throw new IllegalArgumentException("Room type id is required.");
        }
        if (solicitud.precio() == null) {
            throw new IllegalArgumentException("Price is required.");
        }
        Habitacion habitacion = new Habitacion();
        habitacion.setNumero(solicitud.numero());
        habitacion.setPrecio(solicitud.precio());
        habitacion.setEstado(solicitud.estado());
        habitacion.setTipoHabitacion(tipoHabitacionService.obtenerPorId(solicitud.tipoHabitacionId()));
        return habitacion;
    }
}
