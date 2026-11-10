package com.theroyale.backend.service;

import com.theroyale.backend.dto.ChatRequest;
import com.theroyale.backend.dto.ChatResponse;
import com.theroyale.backend.dto.ItinerarioRequest;
import com.theroyale.backend.dto.ItinerarioResponse;
import com.theroyale.backend.model.Servicio;
import com.theroyale.backend.model.TipoHabitacion;
import com.theroyale.backend.repository.ServicioRepository;
import com.theroyale.backend.repository.TipoHabitacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// ===== Servicio de Inteligencia Artificial para el Concierge de The Royale =====
@Service
public class ConciergeAiService {

    @Autowired
    private ServicioRepository servicioRepository;

    @Autowired
    private TipoHabitacionRepository tipoHabitacionRepository;

    public ChatResponse procesarMensaje(ChatRequest request) {
        String msg = request.mensaje() != null ? request.mensaje().toLowerCase(Locale.ROOT).trim() : "";

        // 1. Preguntas sobre Habitaciones / Alojamiento
        if (msg.contains("room") || msg.contains("suite") || msg.contains("habitacion") || msg.contains("stay") || msg.contains("alojamiento")) {
            List<TipoHabitacion> tipos = tipoHabitacionRepository.findAll();
            StringBuilder sb = new StringBuilder();
            sb.append("Welcome to The Royale. We offer a curated collection of refined accommodations:\n\n");
            for (TipoHabitacion t : tipos) {
                sb.append("• **").append(t.getNombre()).append("**: ").append(t.getDescripcion()).append("\n");
            }
            sb.append("\nEvery room features Italian acoustic soundproofing, bespoke Frette linens, and marble bathrooms.");
            return new ChatResponse(
                    sb.toString(),
                    List.of("View Presidential Suite", "Check Room Availability", "Book a Room"),
                    "/rooms",
                    "Explore Accommodations"
            );
        }

        // 2. Preguntas sobre Restaurante / Menú / Dining
        if (msg.contains("food") || msg.contains("din") || msg.contains("restaurant") || msg.contains("menu") || msg.contains("comida") || msg.contains("chef") || msg.contains("wine")) {
            return new ChatResponse(
                    "Our culinary program is led by world-class chefs at **The Royale Signature Restaurant**:\n\n"
                    + "• **Breakfast (06:30 - 11:00)**: Artisanal Viennoiserie, Organic Truffle Eggs Benedict, Fresh Orchard Juices.\n"
                    + "• **All-Day & In-Suite Dining (24h)**: Prime Dry-Aged Angus Ribeye, Wild Atlantic Turbot, Handmade Tagliolini with White Truffle.\n"
                    + "• **Private Wine Cellar**: Over 200 premier grand cru labels curated by our Head Sommelier.\n"
                    + "• **Mixology Lounge**: Signature bespoke smoked cocktails and rare single malts.\n\n"
                    + "Would you like me to reserve a private dining table for your stay?",
                    List.of("Order Room Service", "Private Dining Table", "View Wine List"),
                    "/services",
                    "View Dining Service"
            );
        }

        // 3. Preguntas sobre Spa / Wellness
        if (msg.contains("spa") || msg.contains("wellness") || msg.contains("massage") || msg.contains("relax") || msg.contains("sauna") || msg.contains("gym")) {
            return new ChatResponse(
                    "Indulge at **The Royale Wellness Sanctuary**:\n\n"
                    + "• **Thermal Baths**: Finnish dry sauna (90°C), traditional Turkish hammam, and eucalyptus aromatherapy steam.\n"
                    + "• **Heated Indoor Sky Pool**: Open daily 06:00 to 22:00 with panoramic Manhattan skyline views.\n"
                    + "• **Signature Treatments**: 90-minute Swedish and deep-tissue hot stone massages with organic ESPA oils.\n"
                    + "• **Private Couples Suites**: Exclusive champagne and rose-petal bathing ritual.\n"
                    + "• **Technogym Center**: 24/7 access with private trainers available upon request.",
                    List.of("Book Spa Session", "Couples Massage", "View Pool Hours"),
                    "/services",
                    "Explore Spa & Wellness"
            );
        }

        // 4. Preguntas sobre Itinerario
        if (msg.contains("itinerar") || msg.contains("plan") || msg.contains("what to do") || msg.contains("que hacer") || msg.contains("visit") || msg.contains("tour")) {
            return new ChatResponse(
                    "I would be delighted to orchestrate an unforgettable itinerary for you!\n\n"
                    + "Our **AI Itinerary Planner** customizes your days in Manhattan blending exclusive hotel moments (spa, rooftop cocktails, signature dinner) with curated New York cultural destinations (MoMA, Central Park private carriage, Broadway).\n\n"
                    + "Click below to generate a tailored day-by-day plan with our AI Concierge.",
                    List.of("Plan 2-day romantic stay", "Plan 3-day executive stay", "Family weekend in NYC"),
                    "/concierge",
                    "Open AI Itinerary Planner"
            );
        }

        // 5. Check-in / Check-out / Reglas del hotel
        if (msg.contains("check-in") || msg.contains("check-out") || msg.contains("time") || msg.contains("horario") || msg.contains("policy")) {
            return new ChatResponse(
                    "• **Check-in**: Begins at 15:00 PM (Early check-in upon request).\n"
                    + "• **Check-out**: Up to 12:00 PM noon (Late check-out available for suite guests).\n"
                    + "• **Concierge & Valet**: Available 24 hours at the grand lobby.\n"
                    + "• **High-speed Wi-Fi**: Complimentary 1 Gbps fiber throughout the property.\n\n"
                    + "How else may I assist your upcoming visit?",
                    List.of("Book Airport Transfer", "Luggage Storage", "Contact Front Desk"),
                    "/book",
                    "Reserve Room"
            );
        }

        // Respuesta sofisticada general
        return new ChatResponse(
                "Good day. I am your Royale AI Concierge. I can assist you with suite recommendations, table reservations, spa appointments, and tailor-made New York itineraries.\n\n"
                + "How may I elevate your stay with us today?",
                List.of("Room Recommendations", "Dining & Menus", "Spa Treatments", "Generate NYC Itinerary"),
                "/concierge",
                "Personalized Concierge"
        );
    }

    public ItinerarioResponse generarItinerario(ItinerarioRequest req) {
        int dias = Math.max(1, Math.min(7, req.dias() > 0 ? req.dias() : 3));
        String estilo = req.estilo() != null ? req.estilo() : "Luxury & Romance";
        List<ItinerarioResponse.DiaItinerarioDTO> listaDias = new ArrayList<>();

        for (int i = 1; i <= dias; i++) {
            if (i == 1) {
                listaDias.add(new ItinerarioResponse.DiaItinerarioDTO(
                        1,
                        "Arrival & Manhattan Sunset Welcome",
                        "Morning private luxury transfer from airport. Check-in to your suite, unpack, and enjoy complimentary Dom Pérignon champagne.",
                        "Afternoon stroll through nearby Central Park south paths followed by our Signature Thermal Sauna session at The Royale Spa.",
                        "Evening 5-course tasting menu at The Royale Signature Restaurant with sommelier wine pairing.",
                        "Bespoke turn-down service with artisanal chocolates and aroma therapy mist."
                ));
            } else if (i == 2) {
                listaDias.add(new ItinerarioResponse.DiaItinerarioDTO(
                        2,
                        "Culture, Haute Cuisine & Broadway",
                        "Breakfast in bed or terrace suite dining. Morning private VIP guided tour at The Metropolitan Museum of Art (MoMA).",
                        "Afternoon luxury shopping along Fifth Avenue & Madison Avenue boutiques. Return for an afternoon espresso in the Lobby Lounge.",
                        "Prime orchestra seats at a premier Broadway show, followed by midnight cocktails at The Royale Bar & Lounge.",
                        "Late-night room service with artisan cheese board and vintage port."
                ));
            } else if (i == 3) {
                listaDias.add(new ItinerarioResponse.DiaItinerarioDTO(
                        3,
                        "Total Wellness & Skyline Farewell",
                        "Energizing morning session at the Technogym and heated sky pool overlooking Manhattan waking up.",
                        "60-minute hot-stone couples massage at the spa, followed by a light organic lunch at our wellness terrace.",
                        "Private sunset helicopter tour of the Manhattan skyline, culminating in dinner at an exclusive rooftop.",
                        "Express late check-out arrangements and chauffeur transfer."
                ));
            } else {
                listaDias.add(new ItinerarioResponse.DiaItinerarioDTO(
                        i,
                        "Exclusive New York Gems & Discovery",
                        "Gourmet breakfast at the Grand Salon. Morning architectural private yacht tour around Manhattan harbor.",
                        "Curated exploration of Hudson Yards, The High Line, and Chelsea art galleries with personal concierge escort.",
                        "Private chef dining experience inside your suite or reservations at a 3-star Michelin partner venue.",
                        "Relaxation by the suite fireplace with personal butler service."
                ));
            }
        }

        return new ItinerarioResponse(
                "The Royale Bespoke " + dias + "-Day " + estilo + " Itinerary",
                "A masterfully curated sequence of moments combining The Royale's 5-star hospitality with the finest experiences New York City has to offer.",
                estilo,
                dias,
                listaDias
        );
    }
}
