package com.theroyale.backend.service.plan.fuentes;

import com.theroyale.backend.model.FuentePlan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// ===== Ticketmaster falso con datos fijos: perfil test y plan.fuentes.habilitadas=false =====
// Un concierto de jazz cada noche de 19:30 a 22:30 en Lincoln Center (id "tm-falso-AAAAMMDD").
@Component
@ConditionalOnProperty(prefix = "plan.fuentes", name = "habilitadas", havingValue = "false")
public class TicketmasterEventosClientFalso implements EventosClient {

    @Override
    public FuentePlan fuente() {
        return FuentePlan.TICKETMASTER;
    }

    @Override
    public List<EventoCandidato> buscar(LocalDate inicio, LocalDate fin) {
        List<EventoCandidato> eventos = new ArrayList<>();
        for (LocalDate fecha = inicio; !fecha.isAfter(fin); fecha = fecha.plusDays(1)) {
            eventos.add(new EventoCandidato("tm-falso-" + fecha.toString().replace("-", ""),
                    FuentePlan.TICKETMASTER, "Holiday Jazz Night (sample)",
                    fecha.atTime(19, 30), fecha.atTime(22, 30), "Rose Theater, Jazz at Lincoln Center",
                    40.7685, -73.9822, "MUSICA", "https://www.jazz.org", true));
        }
        return eventos;
    }
}
