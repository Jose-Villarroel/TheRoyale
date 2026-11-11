package com.theroyale.backend.service.plan.fuentes;

import com.theroyale.backend.model.FuentePlan;
import com.theroyale.backend.model.TipoClima;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// ===== NWS falso con datos fijos: perfil test y plan.fuentes.habilitadas=false (nunca sale a internet) =====
// Igual que el real, solo cubre los próximos 7 días: "Partly Sunny", 9 °C / 3 °C y 10 % de lluvia.
@Component
@ConditionalOnProperty(prefix = "plan.fuentes", name = "habilitadas", havingValue = "false")
public class NwsClimaClientFalso implements ClimaClient {

    @Override
    public FuentePlan fuente() {
        return FuentePlan.NWS;
    }

    @Override
    public List<ClimaDia> pronostico(LocalDate inicio, LocalDate fin) {
        LocalDate hoy = LocalDate.now();
        List<ClimaDia> dias = new ArrayList<>();
        for (LocalDate fecha = inicio; !fecha.isAfter(fin); fecha = fecha.plusDays(1)) {
            if (!fecha.isBefore(hoy) && fecha.isBefore(hoy.plusDays(7))) {
                dias.add(new ClimaDia(fecha, "Partly Sunny", 9, 3, 10, TipoClima.PRONOSTICO_NWS));
            }
        }
        return dias;
    }
}
