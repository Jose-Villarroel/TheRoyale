package com.theroyale.backend.service;

import com.theroyale.backend.model.Servicio;

import java.util.List;

// ===== Contrato de la capa de servicio del catálogo de servicios del hotel (spa, restaurante...) =====
// Los controladores dependen de esta interfaz y no de ServicioService, para poder cambiar la implementación.
public interface InterfaceService {

    List<Servicio> listarServicios();

    Servicio buscarPorId(Long id);

    Servicio buscarPorNombre(String nombre);

    Servicio guardar(Servicio servicio);

    // Alta: ignora cualquier id que traiga el objeto, para que nunca sobreescriba uno existente
    Servicio crear(Servicio servicio);

    Servicio actualizar(Long id, Servicio servicio);

    void eliminar(Long id);
}
