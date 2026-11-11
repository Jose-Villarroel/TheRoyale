package com.theroyale.backend.service;

import com.theroyale.backend.dto.PlatoMenuRequest;
import com.theroyale.backend.errors.RecursoNoEncontradoException;
import com.theroyale.backend.model.CategoriaPlato;
import com.theroyale.backend.model.EtiquetaPlato;
import com.theroyale.backend.model.PlatoMenu;
import com.theroyale.backend.model.RestauranteHotel;
import com.theroyale.backend.repository.PlatoMenuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

// ===== Reglas de la carta de los restaurantes: nombre y descripción obligatorios, precio mayor que 0 =====
// La usan el admin del menú (PlatoMenuRestController) y el chatbot (herramienta buscar_platos y prompt).
@Service
@Transactional(readOnly = true)
public class PlatoMenuService {

    private static final int LARGO_MAXIMO_NOMBRE = 100;
    private static final int LARGO_MAXIMO_DESCRIPCION = 500;

    @Autowired
    private PlatoMenuRepository platoMenuRepository;

    // Todos los filtros son opcionales (null = sin filtro). Orden: restaurante, categoría y nombre.
    public List<PlatoMenu> listar(RestauranteHotel restaurante, CategoriaPlato categoria,
                                  EtiquetaPlato etiqueta, boolean soloDisponibles) {
        return platoMenuRepository.buscar(restaurante, categoria, etiqueta, soloDisponibles);
    }

    public PlatoMenu obtenerPorId(Long id) {
        return platoMenuRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Dish not found: " + id));
    }

    @Transactional
    public PlatoMenu crear(PlatoMenuRequest solicitud) {
        validar(solicitud);
        PlatoMenu plato = new PlatoMenu();
        copiarDatos(solicitud, plato);
        return platoMenuRepository.save(plato);
    }

    @Transactional
    public PlatoMenu actualizar(Long id, PlatoMenuRequest solicitud) {
        PlatoMenu plato = obtenerPorId(id);
        validar(solicitud);
        copiarDatos(solicitud, plato);
        return platoMenuRepository.save(plato);
    }

    @Transactional
    public void eliminar(Long id) {
        obtenerPorId(id);
        platoMenuRepository.deleteById(id);
    }

    private void validar(PlatoMenuRequest solicitud) {
        if (solicitud == null) {
            throw new IllegalArgumentException("Dish data is required.");
        }
        if (estaVacio(solicitud.nombre())) {
            throw new IllegalArgumentException("Dish name is required.");
        }
        if (solicitud.nombre().trim().length() > LARGO_MAXIMO_NOMBRE) {
            throw new IllegalArgumentException("Dish name cannot exceed " + LARGO_MAXIMO_NOMBRE + " characters.");
        }
        if (estaVacio(solicitud.descripcion())) {
            throw new IllegalArgumentException("Dish description is required.");
        }
        if (solicitud.descripcion().trim().length() > LARGO_MAXIMO_DESCRIPCION) {
            throw new IllegalArgumentException(
                    "Dish description cannot exceed " + LARGO_MAXIMO_DESCRIPCION + " characters.");
        }
        if (solicitud.precio() == null || solicitud.precio().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be greater than 0.");
        }
        if (solicitud.restaurante() == null) {
            throw new IllegalArgumentException("Restaurant is required.");
        }
        if (solicitud.categoria() == null) {
            throw new IllegalArgumentException("Category is required.");
        }
    }

    // Las @ElementCollection se reemplazan vaciando el conjunto gestionado, no sustituyéndolo
    private void copiarDatos(PlatoMenuRequest solicitud, PlatoMenu plato) {
        plato.setNombre(solicitud.nombre().trim());
        plato.setDescripcion(solicitud.descripcion().trim());
        plato.setPrecio(solicitud.precio());
        plato.setRestaurante(solicitud.restaurante());
        plato.setCategoria(solicitud.categoria());
        plato.getEtiquetas().clear();
        if (solicitud.etiquetas() != null) {
            solicitud.etiquetas().stream()
                    .filter(Objects::nonNull)
                    .forEach(etiqueta -> plato.getEtiquetas().add(etiqueta));
        }
        // Si no se indica, el plato queda disponible
        plato.setDisponible(solicitud.disponible() == null || solicitud.disponible());
        plato.setImagenUrl(estaVacio(solicitud.imagenUrl()) ? null : solicitud.imagenUrl().trim());
    }

    private boolean estaVacio(String texto) {
        return texto == null || texto.trim().isEmpty();
    }
}
