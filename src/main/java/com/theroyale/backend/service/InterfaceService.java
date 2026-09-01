package com.theroyale.backend.service;

import com.theroyale.backend.model.Servicio;

import java.util.List;
import java.util.Optional;

public interface InterfaceService {

    List<Servicio> listarServicios();

    Optional<Servicio> buscarPorId(Long id);

    Optional<Servicio> buscarPorNombre(String nombre);
}
