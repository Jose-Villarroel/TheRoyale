package com.theroyale.backend.repository;

import com.theroyale.backend.model.EstadoReserva;
import com.theroyale.backend.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    boolean existsByClienteId(Long clienteId);

    boolean existsByHabitacionId(Long habitacionId);

    List<Reserva> findAllByOrderByFechaInicioDesc();

    List<Reserva> findByEstadoOrderByFechaInicioDesc(EstadoReserva estado);

    List<Reserva> findByFechaInicioAndEstadoIn(LocalDate fechaInicio, List<EstadoReserva> estados);

    List<Reserva> findByFechaFinAndEstado(LocalDate fechaFin, EstadoReserva estado);

    long countByEstado(EstadoReserva estado);
}
