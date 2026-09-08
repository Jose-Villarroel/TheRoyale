package com.theroyale.backend.service;

import com.theroyale.backend.model.Servicio;

import java.util.List;

public interface InterfaceService {

    List<Servicio> listarServicios();

    Servicio buscarPorId(Long id);

    Servicio buscarPorNombre(String nombre);
}
