package com.theroyale.backend.dto.dashboard;

// ===== Una alerta accionable del panel de dirección =====
// Cada alerta lleva la ruta de la pantalla donde se resuelve (rutaAccion), para que el
// administrador pase de "me avisan" a "lo arreglo" en un clic. Las genera el backend porque las
// reglas son de negocio, no de presentación.
// severidad: ALTA, MEDIA o BAJA; el servicio devuelve la lista ya ordenada por severidad.
public record AlertaResponse(
        String codigo,
        String severidad,
        String titulo,
        String detalle,
        long cantidad,
        String rutaAccion,
        String textoAccion
) {
}
