package com.theroyale.backend.repository;

import com.theroyale.backend.model.Servicio;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class ServicioRepository {

    private final List<Servicio> servicios = new ArrayList<>();

    public ServicioRepository() {
        servicios.add(new Servicio(1L, "Wellness",
                "Immerse yourself in total relaxation at The Royale Spa. Our full wellness sanctuary is designed around your wellbeing - from ancient thermal rituals to modern fitness, every detail is curated for the discerning guest.",
                45.0, "/images/spa.webp",
                List.of(
                        "Heated Indoor Pool - Open daily 06:00 to 22:00",
                        "Finnish Sauna - Dry heat up to 90 degrees, private sessions available",
                        "Turkish Hammam - Traditional steam bath with aromatic oils",
                        "Aromatherapy Steam Room - Eucalyptus and lavender infusions",
                        "Full Gym 24h - Technogym equipment, personal trainer on request",
                        "Signature Massage - 60 or 90 min, Swedish, deep tissue and hot stone",
                        "Organic Facials - Premium skincare with ESPA and La Mer products",
                        "Private Couple Suite - Exclusive treatment room for two"
                ),
                List.of("/images/spa.webp", "/images/architecture.jpg", "/images/new_spa.jpg")
        ));
        servicios.add(new Servicio(2L, "Dining",
                "Experience exceptional cuisine without leaving The Royale. Our signature restaurant serves contemporary New York cuisine from breakfast through late-night dining. In-room service is available 24 hours a day.",
                60.0, "/images/dinningroom-1.webp",
                List.of(
                        "24h Room Service - Full a la carte menu delivered to your suite",
                        "Signature Restaurant - Contemporary New York cuisine, breakfast to dinner",
                        "Private Dining Room - Exclusive setting for up to 12 guests",
                        "Premium Wine List - Over 200 labels from world-renowned vineyards",
                        "Craft Cocktail Bar - Handcrafted cocktails and spirits by our mixologists",
                        "In-Room Minibar - Curated selection refreshed daily",
                        "Private Bar Service - Butler-attended bar set up in your suite",
                        "Dietary Menus - Vegan, gluten-free and allergen-aware options available"
                ),
                List.of("/images/dinningroom-1.webp", "/images/restaurant.jpg", "/images/new_dining.jpg")
        ));
        servicios.add(new Servicio(3L, "Business",
                "Stay productive from the heart of Manhattan. The Royale Business Center delivers everything corporate guests need - from state-of-the-art meeting technology to full executive support, all within steps of your room.",
                35.0, "/images/meeting-room.jpg",
                List.of(
                        "Private Meeting Rooms - Up to 3 rooms, capacity 4 to 20 persons",
                        "4K Video Conferencing - Integrated Zoom and Teams, global connectivity",
                        "High-Speed Fiber Wi-Fi - Dedicated bandwidth up to 1 Gbps",
                        "Executive Lounge - Reserved workspace with panoramic Manhattan views",
                        "Printing and Secretarial - On-demand document handling and admin support",
                        "Business Center - 24h access, iMac workstations and ergonomic seating",
                        "Event Planning - Full AV setup and catering coordination",
                        "Concierge Business Support - Courier, notary and translation services"
                ),
                List.of("/images/meeting-room.jpg", "/images/skyline.jpg", "/images/new_lounge.jpg")
        ));
        servicios.add(new Servicio(4L, "Concierge",
                "Our multilingual concierge team is available around the clock to orchestrate every aspect of your New York experience. No request is too extraordinary - we specialise in turning the impossible into the unforgettable.",
                25.0, "/images/concierge.jpg",
                List.of(
                        "Broadway and Show Tickets - Priority access to sold-out performances",
                        "Fine Dining Reservations - Michelin-starred restaurants and exclusive tables",
                        "Private Museum Tours - After-hours access to MET, MoMA and more",
                        "Helicopter Rides - Scenic Manhattan flights from the East River helipad",
                        "Personal Shopping - Stylist-led experiences on Fifth Avenue",
                        "Airport Transfers - Chauffeured luxury vehicles, 24h availability",
                        "Floral and Gift Arrangements - Bespoke in-suite welcome experiences",
                        "Multilingual Assistance - Staff fluent in 8+ languages"
                ),
                List.of("/images/concierge.jpg", "/images/broadway.jpg", "/images/new_concierge.jpg")
        ));
    }

    public List<Servicio> obtenerTodos() { return servicios; }

    public Optional<Servicio> obtenerPorId(Long id) {
        return servicios.stream().filter(s -> s.getId().equals(id)).findFirst();
    }

    public Optional<Servicio> obtenerPorNombre(String nombre) {
        return servicios.stream().filter(s -> s.getNombre().equalsIgnoreCase(nombre)).findFirst();
    }
}