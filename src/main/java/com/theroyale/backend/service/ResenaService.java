package com.theroyale.backend.service;

import com.theroyale.backend.dto.CrearResenaRequest;
import com.theroyale.backend.dto.ResenaResponse;
import com.theroyale.backend.dto.ResumenResenasResponse;
import com.theroyale.backend.errors.RecursoNoEncontradoException;
import com.theroyale.backend.model.Cliente;
import com.theroyale.backend.model.Resena;
import com.theroyale.backend.model.TipoHabitacion;
import com.theroyale.backend.repository.ClienteRepository;
import com.theroyale.backend.repository.ResenaRepository;
import com.theroyale.backend.repository.TipoHabitacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ResenaService {

    @Autowired
    private ResenaRepository resenaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private TipoHabitacionRepository tipoHabitacionRepository;

    public ResumenResenasResponse obtenerResumenPorTipo(Long tipoId) {
        List<Resena> resenas = resenaRepository.findByTipoHabitacionIdOrderByFechaDesc(tipoId);
        long total = resenas.size();
        Double promedio = resenaRepository.calcularPromedioEstrellas(tipoId);
        double promedioRedondeado = promedio != null
                ? BigDecimal.valueOf(promedio).setScale(1, RoundingMode.HALF_UP).doubleValue()
                : 5.0;

        Map<Integer, Long> conteo = resenas.stream()
                .collect(Collectors.groupingBy(Resena::getEstrellas, Collectors.counting()));

        List<ResenaResponse> dtoList = resenas.stream()
                .map(ResenaResponse::from)
                .toList();

        return new ResumenResenasResponse(promedioRedondeado, total, conteo, dtoList);
    }

    public List<ResenaResponse> listarRecientes() {
        return resenaRepository.findAllByOrderByFechaDesc().stream()
                .limit(6)
                .map(ResenaResponse::from)
                .toList();
    }

    @Transactional
    public ResenaResponse crearResena(CrearResenaRequest request) {
        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado con id " + request.clienteId()));

        TipoHabitacion tipo = tipoHabitacionRepository.findById(request.tipoHabitacionId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Tipo de habitación no encontrado con id " + request.tipoHabitacionId()));

        int estrellas = Math.max(1, Math.min(5, request.estrellas()));

        Resena resena = Resena.builder()
                .cliente(cliente)
                .tipoHabitacion(tipo)
                .estrellas(estrellas)
                .titulo(request.titulo() != null && !request.titulo().isBlank() ? request.titulo().trim() : "Verified Guest Experience")
                .comentario(request.comentario().trim())
                .fecha(LocalDateTime.now())
                .build();

        Resena guardada = resenaRepository.save(resena);
        return ResenaResponse.from(guardada);
    }
}
