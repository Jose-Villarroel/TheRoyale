package com.theroyale.backend.service.plan.fuentes;

import com.theroyale.backend.model.FuentePlan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// ===== NYC Open Data falso con datos fijos: perfil test y plan.fuentes.habilitadas=false =====
// Un mercado en Union Square cada día de la estadía, de 08:00 a 18:00 (id "nyc-falso-AAAAMMDD").
@Component
@ConditionalOnProperty(prefix = "plan.fuentes", name = "habilitadas", havingValue = "false")
public class NycOpenDataEventosClientFalso implements EventosClient {

    @Override
    public FuentePlan fuente() {
        return FuentePlan.NYC_OPEN_DATA;
    }

    @Override
    public List<EventoCandidato> buscar(LocalDate inicio, LocalDate fin) {
        List<EventoCandidato> eventos = new ArrayList<>();
        for (LocalDate fecha = inicio; !fecha.isAfter(fin); fecha = fecha.plusDays(1)) {
            eventos.add(new EventoCandidato("nyc-falso-" + fecha.toString().replace("-", ""),
                    FuentePlan.NYC_OPEN_DATA, "Union Square Greenmarket (sample)",
                    fecha.atTime(8, 0), fecha.atTime(18, 0), "Union Square Park",
                    40.7359, -73.9911, "FERIA", NycOpenDataEventosClient.URL_FICHA, false));
        }
        return eventos;
    }
}
