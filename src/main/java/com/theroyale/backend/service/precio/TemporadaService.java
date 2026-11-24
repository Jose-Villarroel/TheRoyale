package com.theroyale.backend.service.precio;

import com.theroyale.backend.dto.TemporadaRequest;
import com.theroyale.backend.errors.RecursoNoEncontradoException;
import com.theroyale.backend.model.Temporada;
import com.theroyale.backend.repository.TemporadaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// ===== Temporadas de precios (CRUD del admin) y factor de temporada de una noche =====
@Service
@Transactional(readOnly = true)
public class TemporadaService {

    public static final double FACTOR_MINIMO = 0.5;
    public static final double FACTOR_MAXIMO = 2.0;

    private final TemporadaRepository temporadaRepository;

    public TemporadaService(TemporadaRepository temporadaRepository) {
        this.temporadaRepository = temporadaRepository;
    }

    public List<Temporada> listar() {
        return temporadaRepository.findAllByOrderByFechaInicioAsc();
    }

    public Temporada obtenerPorId(Long id) {
        return temporadaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Season not found: " + id));
    }

    @Transactional
    public Temporada crear(TemporadaRequest solicitud) {
        validar(solicitud);
        return temporadaRepository.save(Temporada.builder()
                .nombre(solicitud.nombre().trim())
                .fechaInicio(solicitud.fechaInicio())
                .fechaFin(solicitud.fechaFin())
                .factor(solicitud.factor())
                .build());
    }

    @Transactional
    public Temporada actualizar(Long id, TemporadaRequest solicitud) {
        validar(solicitud);
        Temporada temporada = obtenerPorId(id);
        temporada.setNombre(solicitud.nombre().trim());
        temporada.setFechaInicio(solicitud.fechaInicio());
        temporada.setFechaFin(solicitud.fechaFin());
        temporada.setFactor(solicitud.factor());
        return temporadaRepository.save(temporada);
    }

    @Transactional
    public void eliminar(Long id) {
        temporadaRepository.delete(obtenerPorId(id));
    }

    // ===== Temporada que manda en una noche: si hay varias, la de factor más alejado de 1 =====
    public Optional<Temporada> temporadaDe(LocalDate noche, List<Temporada> temporadas) {
        return temporadas.stream()
                .filter(t -> t.cubre(noche))
                .max((a, b) -> Double.compare(Math.abs(a.getFactor() - 1), Math.abs(b.getFactor() - 1)));
    }

    private void validar(TemporadaRequest s) {
        if (s == null || s.nombre() == null || s.nombre().isBlank() || s.fechaInicio() == null
                || s.fechaFin() == null || s.factor() == null) {
            throw new IllegalArgumentException("Name, dates and factor are required.");
        }
        if (s.nombre().trim().length() > 80) {
            throw new IllegalArgumentException("The season name can have at most 80 characters.");
        }
        if (s.fechaFin().isBefore(s.fechaInicio())) {
            throw new IllegalArgumentException("The season cannot end before it starts.");
        }
        if (s.factor() < FACTOR_MINIMO || s.factor() > FACTOR_MAXIMO) {
            throw new IllegalArgumentException("The factor must be between 0.5 and 2.0.");
        }
    }
}
