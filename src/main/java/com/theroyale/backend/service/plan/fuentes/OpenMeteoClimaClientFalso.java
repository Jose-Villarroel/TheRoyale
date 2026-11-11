package com.theroyale.backend.service.plan.fuentes;

import com.theroyale.backend.model.FuentePlan;
import com.theroyale.backend.model.TipoClima;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// ===== Open-Meteo falso con datos fijos: perfil test y plan.fuentes.habilitadas=false =====
// Pronóstico "Cloudy" para los 16 días que cubre el real y clima típico "Cold, usually dry" para el resto.
@Component
@ConditionalOnProperty(prefix = "plan.fuentes", name = "habilitadas", havingValue = "false")
public class OpenMeteoClimaClientFalso implements ClimaClient {

    @Override
    public FuentePlan fuente() {
        return FuentePlan.OPEN_METEO;
    }

    @Override
    public List<ClimaDia> pronostico(LocalDate inicio, LocalDate fin) {
        LocalDate limite = LocalDate.now().plusDays(16);
        List<ClimaDia> dias = new ArrayList<>();
        for (LocalDate fecha = inicio; !fecha.isAfter(fin) && fecha.isBefore(limite); fecha = fecha.plusDays(1)) {
            dias.add(new ClimaDia(fecha, "Cloudy", 8, 2, 20, TipoClima.PRONOSTICO_OPEN_METEO));
        }
        return dias;
    }

    @Override
    public List<ClimaDia> climaTipico(LocalDate inicio, LocalDate fin) {
        List<ClimaDia> dias = new ArrayList<>();
        for (LocalDate fecha = inicio; !fecha.isAfter(fin); fecha = fecha.plusDays(1)) {
            dias.add(new ClimaDia(fecha, "Cold, usually dry", 6, 0, 33, TipoClima.CLIMA_TIPICO));
        }
        return dias;
    }
}
