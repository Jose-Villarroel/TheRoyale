package com.theroyale.backend.service.ocupacion;

import com.theroyale.backend.model.ReservaHistorica;
import com.theroyale.backend.model.TipoHabitacion;
import com.theroyale.backend.repository.ReservaHistoricaRepository;
import com.theroyale.backend.repository.TipoHabitacionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// ===== Carga el historial simulado de reservas (reservas-historicas.csv) =====
// El CSV se generó con fechas fijas que terminan el día antes de la fecha de referencia del modelo.
// Al cargarlo, todas las fechas se corren para que esa referencia sea "hoy": así el historial
// siempre termina ayer y las reservas futuras simuladas caen en los próximos 90 días.
@Service
public class HistoricoOcupacionService {

    private final ReservaHistoricaRepository reservaHistoricaRepository;
    private final TipoHabitacionRepository tipoHabitacionRepository;
    private final ModeloOcupacion modelo;
    private final OcupacionService ocupacionService;
    private final String recurso;

    public HistoricoOcupacionService(ReservaHistoricaRepository reservaHistoricaRepository,
                                     TipoHabitacionRepository tipoHabitacionRepository,
                                     ModeloOcupacion modelo, OcupacionService ocupacionService,
                                     @Value("${ocupacion.historico.recurso:modelo/reservas-historicas.csv}") String recurso) {
        this.reservaHistoricaRepository = reservaHistoricaRepository;
        this.tipoHabitacionRepository = tipoHabitacionRepository;
        this.modelo = modelo;
        this.ocupacionService = ocupacionService;
        this.recurso = recurso;
    }

    // Devuelve cuántas reservas se cargaron. Tipos que no existan en la base se ignoran.
    @Transactional
    public int cargar(LocalDate hoy) {
        long desplazamiento = ChronoUnit.DAYS.between(modelo.getEntrenadoEl(), hoy);
        Map<String, TipoHabitacion> tipos = tipoHabitacionRepository.findAll().stream()
                .collect(Collectors.toMap(TipoHabitacion::getNombre, Function.identity(), (a, b) -> a));

        List<ReservaHistorica> reservas = new ArrayList<>();
        try (BufferedReader lector = new BufferedReader(
                new InputStreamReader(new ClassPathResource(recurso).getInputStream(), StandardCharsets.UTF_8))) {
            lector.readLine(); // cabecera
            String linea;
            while ((linea = lector.readLine()) != null) {
                String[] c = linea.split(",");
                TipoHabitacion tipo = tipos.get(c[3]);
                if (c.length < 5 || tipo == null) {
                    continue;
                }
                reservas.add(ReservaHistorica.builder()
                        .fechaCreacion(LocalDate.parse(c[0]).plusDays(desplazamiento))
                        .llegada(LocalDate.parse(c[1]).plusDays(desplazamiento))
                        .noches(Integer.parseInt(c[2]))
                        .tipoHabitacion(tipo)
                        .cancelada(Boolean.parseBoolean(c[4]))
                        .build());
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("The occupancy history " + recurso + " could not be read.", ex);
        }
        reservaHistoricaRepository.saveAll(reservas);
        ocupacionService.olvidarHistorico();
        return reservas.size();
    }
}
