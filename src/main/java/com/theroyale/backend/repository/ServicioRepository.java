package com.theroyale.backend.repository;

import com.theroyale.backend.model.Servicio;
import org.springframework.stereotype.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class ServicioRepository {

    private final List<Servicio> servicios = new ArrayList<>();

    public ServicioRepository() {
        servicios.add(new Servicio(
                1L,
                "Wellness",
                "Spa, sauna, Turkish bath, pool and gym.",
                45.0,
                "/images/spa.webp"
        ));
        servicios.add(new Servicio(
                2L,
                "Dining",
                "Room service, restaurant, bar and minibar.",
                60.0,
                "/images/dinningroom-1.webp"
        ));
        servicios.add(new Servicio(
                3L,
                "Business",
                "Business center, meeting rooms and executive services.",
                35.0,
                "/images/meeting-room.jpg"
        ));
        servicios.add(new Servicio(
                4L,
                "Concierge",
                "Personalized assistance and premium guest services.",
                25.0,
                "/images/concierge.jpg"
        ));
    }

    public List<Servicio> obtenerTodos() {
        return servicios;
    }

    public Optional<Servicio> obtenerPorId(Long id) {
        return servicios.stream()
                .filter(servicio -> servicio.getId().equals(id))
                .findFirst();
    }

    public Optional<Servicio> obtenerPorNombre(String nombre) {
        return servicios.stream()
                .filter(servicio -> servicio.getNombre().equalsIgnoreCase(nombre))
                .findFirst();
    }
}