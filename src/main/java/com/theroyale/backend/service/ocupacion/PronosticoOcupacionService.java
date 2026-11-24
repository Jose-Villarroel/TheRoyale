package com.theroyale.backend.service.ocupacion;

import com.theroyale.backend.dto.PronosticoOcupacionResponse;
import com.theroyale.backend.dto.PronosticoOcupacionResponse.FechaAltaDemanda;
import com.theroyale.backend.dto.PronosticoOcupacionResponse.FichaModelo;
import com.theroyale.backend.dto.PronosticoOcupacionResponse.Kpis;
import com.theroyale.backend.dto.PronosticoOcupacionResponse.Punto;
import com.theroyale.backend.dto.PronosticoOcupacionResponse.TipoResumen;
import com.theroyale.backend.errors.RecursoNoEncontradoException;
import com.theroyale.backend.model.TipoHabitacion;
import com.theroyale.backend.repository.TipoHabitacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

// ===== Pronóstico de ocupación de los próximos días para el admin (funcionalidad 9) =====
// Por cada noche y tipo arma las variables del modelo con lo que hay reservado HOY y la ocupación
// del mismo día un año antes (fecha - 364, mismo día de la semana). El hotel completo es el
// promedio de los tipos ponderado por su número de habitaciones.
@Service
@Transactional(readOnly = true)
public class PronosticoOcupacionService {

    public static final ZoneId ZONA_NUEVA_YORK = ZoneId.of("America/New_York");
    public static final double UMBRAL_ALTA_DEMANDA = 0.85;
    private static final int DIAS_PASADOS = 30;

    private final OcupacionService ocupacionService;
    private final ModeloOcupacion modelo;
    private final TipoHabitacionRepository tipoHabitacionRepository;
    private final Clock reloj;

    @Autowired
    public PronosticoOcupacionService(OcupacionService ocupacionService, ModeloOcupacion modelo,
                                      TipoHabitacionRepository tipoHabitacionRepository) {
        this(ocupacionService, modelo, tipoHabitacionRepository, Clock.system(ZONA_NUEVA_YORK));
    }

    // Para pruebas: un reloj fijo
    PronosticoOcupacionService(OcupacionService ocupacionService, ModeloOcupacion modelo,
                               TipoHabitacionRepository tipoHabitacionRepository, Clock reloj) {
        this.ocupacionService = ocupacionService;
        this.modelo = modelo;
        this.tipoHabitacionRepository = tipoHabitacionRepository;
        this.reloj = reloj;
    }

    // ===== Pronóstico de una noche para un tipo, con la foto ya armada =====
    public double pronosticar(OcupacionService.Foto foto, TipoHabitacion tipo, LocalDate noche, LocalDate hoy) {
        long diasAntes = ChronoUnit.DAYS.between(hoy, noche);
        double reservada = foto.reservada(tipo.getId(), noche, hoy);
        double anioAnterior = foto.ocupacionFinal(tipo.getId(), noche.minusDays(364));
        return modelo.predecir(ModeloOcupacion.variables(reservada, diasAntes, anioAnterior, noche, tipo.getNombre()));
    }

    // ===== Ocupación esperada que usan las recomendaciones de precio =====
    // La mayor entre lo que ya está reservado (vigente) y lo pronosticado: si ya hay más reservado
    // que lo que predice el modelo, manda lo reservado.
    public double ocupacionEsperada(OcupacionService.Foto foto, TipoHabitacion tipo, LocalDate noche, LocalDate hoy) {
        return Math.max(foto.reservadaVigente(tipo.getId(), noche, hoy), pronosticar(foto, tipo, noche, hoy));
    }

    public LocalDate hoy() {
        return LocalDate.now(reloj);
    }

    public PronosticoOcupacionResponse pronostico(int dias, Long tipoHabitacionId) {
        if (dias < 7 || dias > 90) {
            throw new IllegalArgumentException("The forecast covers between 7 and 90 days.");
        }
        LocalDate hoy = LocalDate.now(reloj);
        List<TipoHabitacion> todos = tipoHabitacionRepository.findAll();
        List<TipoHabitacion> alcance = todos;
        if (tipoHabitacionId != null) {
            alcance = todos.stream().filter(t -> t.getId().equals(tipoHabitacionId)).toList();
            if (alcance.isEmpty()) {
                throw new RecursoNoEncontradoException("Room type not found: " + tipoHabitacionId);
            }
        }
        OcupacionService.Foto foto = ocupacionService.foto();
        int totalHabitaciones = alcance.stream().mapToInt(t -> foto.habitaciones(t.getId())).sum();

        List<Punto> serie = new ArrayList<>();
        double sumaReal = 0;
        for (int i = DIAS_PASADOS; i >= 1; i--) {
            LocalDate noche = hoy.minusDays(i);
            double real = ponderado(alcance, foto, totalHabitaciones, t -> foto.ocupacionFinal(t.getId(), noche));
            sumaReal += real;
            serie.add(new Punto(noche, redondear(real), null, null, null, null));
        }

        List<FechaAltaDemanda> altaDemanda = new ArrayList<>();
        double suma7 = 0, suma30 = 0, sumaReservada30 = 0;
        for (int i = 0; i < dias; i++) {
            LocalDate noche = hoy.plusDays(i);
            double pronostico = ponderado(alcance, foto, totalHabitaciones, t -> pronosticar(foto, t, noche, hoy));
            double reservada = ponderado(alcance, foto, totalHabitaciones, t -> foto.reservadaVigente(t.getId(), noche, hoy));
            pronostico = Math.max(pronostico, reservada);
            double error = modelo.errorEsperado(i);
            serie.add(new Punto(noche, null, redondear(reservada), redondear(pronostico),
                    redondear(Math.max(reservada, pronostico - error)), redondear(Math.min(1.0, pronostico + error))));
            if (i < 7) {
                suma7 += pronostico;
            }
            if (i < 30) {
                suma30 += pronostico;
                sumaReservada30 += reservada;
            }
            if (pronostico >= UMBRAL_ALTA_DEMANDA) {
                altaDemanda.add(new FechaAltaDemanda(noche, redondear(pronostico), FeriadosNyc.esAltaDemanda(noche)));
            }
        }
        int dias30 = Math.min(30, dias);

        List<TipoResumen> tipos = todos.stream()
                .map(t -> new TipoResumen(t.getId(), t.getNombre(), foto.habitaciones(t.getId())))
                .toList();
        String nombreAlcance = tipoHabitacionId == null ? "Whole hotel" : alcance.get(0).getNombre();
        Kpis kpis = new Kpis(redondear(suma7 / 7), redondear(suma30 / dias30),
                redondear(sumaReservada30 / dias30), redondear(sumaReal / DIAS_PASADOS));
        FichaModelo ficha = new FichaModelo(modelo.getAlgoritmo(), modelo.getEntrenadoEl(), modelo.getFuente(),
                modelo.getMaeModelo(), modelo.getMaeIngenuo(), modelo.getMaePorAnticipacion(),
                modelo.getFilasEntrenamiento());
        return new PronosticoOcupacionResponse(tipoHabitacionId, nombreAlcance, tipos, serie, kpis, altaDemanda, ficha);
    }

    private interface PorTipo {
        double valor(TipoHabitacion tipo);
    }

    private double ponderado(List<TipoHabitacion> tipos, OcupacionService.Foto foto, int totalHabitaciones, PorTipo f) {
        if (totalHabitaciones == 0) {
            return 0.0;
        }
        double suma = 0;
        for (TipoHabitacion tipo : tipos) {
            suma += f.valor(tipo) * foto.habitaciones(tipo.getId());
        }
        return suma / totalHabitaciones;
    }

    private static double redondear(double valor) {
        return Math.round(valor * 1000) / 1000.0;
    }
}
