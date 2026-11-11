package com.theroyale.backend.service;

import com.theroyale.backend.dto.CrearResenaRequest;
import com.theroyale.backend.dto.ResumenResenasResponse;
import com.theroyale.backend.errors.AccesoDenegadoException;
import com.theroyale.backend.errors.RecursoNoEncontradoException;
import com.theroyale.backend.model.EstadoReserva;
import com.theroyale.backend.model.Resena;
import com.theroyale.backend.model.Reserva;
import com.theroyale.backend.model.TipoHabitacion;
import com.theroyale.backend.repository.ResenaRepository;
import com.theroyale.backend.repository.ReservaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// ===== Reglas de las reseñas de habitaciones (RN-1 a RN-10 de Docs/especificacion-resenas.md) =====
// El huésped reseña una estadía FINALIZADA propia; la reseña se asocia al tipo de habitación, que es
// lo que ve el público. El admin puede ocultarla: deja de verse y de contar para el promedio.
//
// Dependencias: la reserva se carga con ReservaRepository y no con ReservaService, porque
// ReservaService depende de este servicio (eliminar() borra la reseña antes que la reserva, RN-9).
// Así la dependencia va en un solo sentido (ReservaService -> ResenaService) y no hace falta @Lazy.
// Es el mismo criterio que ya sigue ClienteService, que usa ReservaRepository para sus validaciones.
@Service
@Transactional(readOnly = true)
public class ResenaService {

    public static final int CALIFICACION_MINIMA = 1;
    public static final int CALIFICACION_MAXIMA = 5;
    public static final int COMENTARIO_MINIMO = 10;
    public static final int COMENTARIO_MAXIMO = 1000;
    public static final int TITULO_MAXIMO = 100;

    @Autowired
    private ResenaRepository resenaRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private TipoHabitacionService tipoHabitacionService;

    // ===== Consultas =====

    public Resena obtenerPorId(Long id) {
        return resenaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Review not found: " + id));
    }

    // Público: solo las visibles del tipo, de la más reciente a la más antigua (RN-8)
    public List<Resena> listarPorTipo(Long tipoHabitacionId) {
        tipoHabitacionService.obtenerPorId(tipoHabitacionId);
        return resenaRepository.findByTipoHabitacionIdAndVisibleTrueOrderByFechaCreacionDesc(tipoHabitacionId);
    }

    // El autor sigue viendo sus reseñas ocultas en "My reservations" (RN-8)
    public List<Resena> listarPorCliente(Long clienteId) {
        clienteService.obtenerPorId(clienteId);
        return resenaRepository.findByClienteIdOrderByFechaCreacionDesc(clienteId);
    }

    // Admin: todas, visibles y ocultas, con filtros opcionales por tipo y por estrellas
    public List<Resena> listarParaModeracion(Long tipoHabitacionId, Integer calificacion) {
        if (calificacion != null) {
            validarCalificacion(calificacion);
        }
        return resenaRepository.buscarParaModeracion(tipoHabitacionId, calificacion);
    }

    // ===== Resumen de un tipo: promedio, total y distribución, solo con las visibles =====
    public ResumenResenasResponse resumen(Long tipoHabitacionId) {
        tipoHabitacionService.obtenerPorId(tipoHabitacionId);

        Map<Integer, Long> distribucion = distribucionVacia();
        for (Object[] fila : resenaRepository.contarPorCalificacion(tipoHabitacionId)) {
            distribucion.put(((Number) fila[0]).intValue(), ((Number) fila[1]).longValue());
        }

        long total = 0;
        long suma = 0;
        for (Map.Entry<Integer, Long> entrada : distribucion.entrySet()) {
            total += entrada.getValue();
            suma += (long) entrada.getKey() * entrada.getValue();
        }
        Double promedio = total == 0 ? null : redondear((double) suma / total);
        return new ResumenResenasResponse(tipoHabitacionId, promedio, total, distribucion);
    }

    // ===== Resumen de todos los tipos (para /rooms), incluidos los que no tienen reseñas =====
    // Dos consultas en total: promedio y total por tipo, y la distribución por tipo y estrellas.
    public List<ResumenResenasResponse> resumenTodos() {
        Map<Long, Object[]> promediosPorTipo = new HashMap<>();
        for (Object[] fila : resenaRepository.resumenPorTipo()) {
            promediosPorTipo.put(((Number) fila[0]).longValue(), fila);
        }

        Map<Long, Map<Integer, Long>> distribucionesPorTipo = new HashMap<>();
        for (Object[] fila : resenaRepository.contarPorTipoYCalificacion()) {
            distribucionesPorTipo
                    .computeIfAbsent(((Number) fila[0]).longValue(), id -> distribucionVacia())
                    .put(((Number) fila[1]).intValue(), ((Number) fila[2]).longValue());
        }

        return tipoHabitacionService.listarTodos().stream()
                .map(tipo -> {
                    Object[] fila = promediosPorTipo.get(tipo.getId());
                    Map<Integer, Long> distribucion =
                            distribucionesPorTipo.getOrDefault(tipo.getId(), distribucionVacia());
                    if (fila == null) {
                        return new ResumenResenasResponse(tipo.getId(), null, 0, distribucion);
                    }
                    return new ResumenResenasResponse(tipo.getId(),
                            redondear(((Number) fila[1]).doubleValue()),
                            ((Number) fila[2]).longValue(),
                            distribucion);
                })
                .toList();
    }

    // ===== Crea la reseña de una estadía terminada =====
    // Orden de validación: datos del formulario (400), reserva existente (404), dueño (403),
    // estado FINALIZADA (409) y que no tenga ya reseña (409).
    @Transactional
    public Resena crear(CrearResenaRequest solicitud) {
        if (solicitud == null || solicitud.reservaId() == null || solicitud.clienteId() == null) {
            throw new IllegalArgumentException("Some required data is missing to create the review.");
        }
        validarContenido(solicitud);

        Reserva reserva = reservaRepository.findById(solicitud.reservaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada: " + solicitud.reservaId()));

        // RN-2
        if (reserva.getCliente() == null || !reserva.getCliente().getId().equals(solicitud.clienteId())) {
            throw new AccesoDenegadoException("This reservation does not belong to you.");
        }
        // RN-1
        if (reserva.getEstado() != EstadoReserva.FINALIZADA) {
            throw new IllegalStateException("Only completed stays can be reviewed.");
        }
        // RN-3
        if (resenaRepository.existsByReservaId(reserva.getId())) {
            throw new IllegalStateException("This reservation already has a review.");
        }

        // RN-6: el tipo se copia de la reserva en este momento; si luego la habitación cambia de tipo,
        // la reseña sigue perteneciendo al tipo en que el huésped se hospedó
        TipoHabitacion tipo = reserva.getHabitacion().getTipoHabitacion();
        Resena resena = Resena.builder()
                .reserva(reserva)
                .cliente(reserva.getCliente())
                .tipoHabitacion(tipo)
                .calificacion(solicitud.calificacion())
                .titulo(limpiarTitulo(solicitud.titulo()))
                .comentario(solicitud.comentario().trim())
                .visible(true)
                .fechaCreacion(LocalDateTime.now())
                .build();

        Resena guardada = resenaRepository.save(resena);
        reserva.setResena(guardada);
        return guardada;
    }

    // ===== El autor cambia estrellas, título y comentario (RN-7); reserva y tipo no cambian =====
    @Transactional
    public Resena actualizar(Long id, CrearResenaRequest cambios) {
        if (cambios == null || cambios.clienteId() == null) {
            throw new IllegalArgumentException("Some required data is missing to update the review.");
        }
        validarContenido(cambios);

        Resena resena = obtenerPorId(id);
        exigirAutor(resena, cambios.clienteId());

        resena.setCalificacion(cambios.calificacion());
        resena.setTitulo(limpiarTitulo(cambios.titulo()));
        resena.setComentario(cambios.comentario().trim());
        resena.setFechaModificacion(LocalDateTime.now());
        return resenaRepository.save(resena);
    }

    @Transactional
    public void eliminar(Long id, Long clienteId) {
        Resena resena = obtenerPorId(id);
        exigirAutor(resena, clienteId);
        borrar(resena);
    }

    // ===== Moderación del admin (RN-8) =====
    @Transactional
    public Resena ocultar(Long id) {
        return cambiarVisibilidad(id, false);
    }

    @Transactional
    public Resena mostrar(Long id) {
        return cambiarVisibilidad(id, true);
    }

    // ===== La dispara ReservaService.eliminar antes de borrar la reserva (RN-9) =====
    // Se hace flush para que el DELETE de la reseña salga antes que el de la reserva (FK reserva_id).
    @Transactional
    public void eliminarPorReserva(Reserva reserva) {
        resenaRepository.findByReservaId(reserva.getId()).ifPresent(this::borrar);
    }

    private void borrar(Resena resena) {
        if (resena.getReserva() != null) {
            resena.getReserva().setResena(null);
        }
        resenaRepository.delete(resena);
        resenaRepository.flush();
    }

    private Resena cambiarVisibilidad(Long id, boolean visible) {
        Resena resena = obtenerPorId(id);
        resena.setVisible(visible);
        return resenaRepository.save(resena);
    }

    // RN-7: solo el autor edita o elimina
    private void exigirAutor(Resena resena, Long clienteId) {
        if (clienteId == null || resena.getCliente() == null || !resena.getCliente().getId().equals(clienteId)) {
            throw new AccesoDenegadoException("Only the author can edit or delete this review.");
        }
    }

    // RN-4 y RN-5
    private void validarContenido(CrearResenaRequest datos) {
        validarCalificacion(datos.calificacion());

        String comentario = datos.comentario() == null ? "" : datos.comentario().trim();
        if (comentario.length() < COMENTARIO_MINIMO || comentario.length() > COMENTARIO_MAXIMO) {
            throw new IllegalArgumentException("The comment is required and must be between "
                    + COMENTARIO_MINIMO + " and " + COMENTARIO_MAXIMO + " characters.");
        }

        String titulo = limpiarTitulo(datos.titulo());
        if (titulo != null && titulo.length() > TITULO_MAXIMO) {
            throw new IllegalArgumentException("The title cannot exceed " + TITULO_MAXIMO + " characters.");
        }
    }

    private void validarCalificacion(Integer calificacion) {
        if (calificacion == null || calificacion < CALIFICACION_MINIMA || calificacion > CALIFICACION_MAXIMA) {
            throw new IllegalArgumentException("The rating must be a whole number between "
                    + CALIFICACION_MINIMA + " and " + CALIFICACION_MAXIMA + ".");
        }
    }

    // El título es opcional: uno en blanco se guarda como null
    private String limpiarTitulo(String titulo) {
        if (titulo == null || titulo.trim().isEmpty()) {
            return null;
        }
        return titulo.trim();
    }

    // Claves 1..5 siempre presentes y en orden, con 0 si no hay reseñas de esas estrellas
    private Map<Integer, Long> distribucionVacia() {
        Map<Integer, Long> distribucion = new LinkedHashMap<>();
        for (int estrellas = CALIFICACION_MINIMA; estrellas <= CALIFICACION_MAXIMA; estrellas++) {
            distribucion.put(estrellas, 0L);
        }
        return distribucion;
    }

    // RN-10: 1 decimal, redondeo "half up" (4.25 -> 4.3)
    private Double redondear(double valor) {
        return BigDecimal.valueOf(valor).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
