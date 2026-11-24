package com.theroyale.backend.service.precio;

import com.theroyale.backend.model.Habitacion;
import com.theroyale.backend.model.TarifaAplicada;
import com.theroyale.backend.repository.TarifaAplicadaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

// ===== Precio real de una habitación por noche (funcionalidad 10) =====
// Precio base de la habitación x factor de la TarifaAplicada vigente esa noche (la que aprobó el
// admin). Sin tarifa aprobada, el precio base sin cambios. Nada se modifica sin aprobación.
// Una estadía cobra cada noche con su precio; precioNocheAcordado guarda el promedio.
@Service
@Transactional(readOnly = true)
public class PrecioService {

    private static final BigDecimal CINCO = BigDecimal.valueOf(5);

    private final TarifaAplicadaRepository tarifaAplicadaRepository;

    public PrecioService(TarifaAplicadaRepository tarifaAplicadaRepository) {
        this.tarifaAplicadaRepository = tarifaAplicadaRepository;
    }

    // Factor aprobado para un tipo en una noche (1 = precio base). Si se superponen, la más reciente.
    public double factorVigente(Long tipoHabitacionId, LocalDate noche) {
        return tarifaVigente(tarifaAplicadaRepository.findByTipoHabitacionId(tipoHabitacionId), noche)
                .map(TarifaAplicada::getFactor)
                .orElse(1.0);
    }

    public BigDecimal precioNoche(Habitacion habitacion, LocalDate noche) {
        return aplicarFactor(habitacion.getPrecio(), factorVigente(habitacion.getTipoHabitacion().getId(), noche));
    }

    // ===== Precio promedio por noche de una estadía [inicio, fin) =====
    public BigDecimal precioPromedio(Habitacion habitacion, LocalDate inicio, LocalDate fin) {
        long noches = ChronoUnit.DAYS.between(inicio, fin);
        if (noches <= 0) {
            return BigDecimal.valueOf(habitacion.getPrecio()).setScale(2, RoundingMode.HALF_UP);
        }
        List<TarifaAplicada> tarifas = tarifaAplicadaRepository.findByTipoHabitacionId(habitacion.getTipoHabitacion().getId());
        BigDecimal total = BigDecimal.ZERO;
        for (LocalDate noche = inicio; noche.isBefore(fin); noche = noche.plusDays(1)) {
            double factor = tarifaVigente(tarifas, noche).map(TarifaAplicada::getFactor).orElse(1.0);
            total = total.add(aplicarFactor(habitacion.getPrecio(), factor));
        }
        return total.divide(BigDecimal.valueOf(noches), 2, RoundingMode.HALF_UP);
    }

    // ===== Etiqueta para el huésped si alguna noche de la estadía tiene precio especial =====
    // Devuelve la de la noche con el factor más alejado de 1 ("High season", "High demand"...), o null.
    public String etiqueta(Long tipoHabitacionId, LocalDate inicio, LocalDate fin) {
        List<TarifaAplicada> tarifas = tarifaAplicadaRepository.findByTipoHabitacionId(tipoHabitacionId);
        TarifaAplicada elegida = null;
        for (LocalDate noche = inicio; noche.isBefore(fin); noche = noche.plusDays(1)) {
            TarifaAplicada t = tarifaVigente(tarifas, noche).orElse(null);
            if (t != null && (elegida == null || Math.abs(t.getFactor() - 1) > Math.abs(elegida.getFactor() - 1))) {
                elegida = t;
            }
        }
        return elegida == null ? null : elegida.getEtiqueta();
    }

    // ===== Precio base x factor, redondeado a múltiplos de $5 (sin tocar el base si el factor es 1) =====
    public static BigDecimal aplicarFactor(double precioBase, double factor) {
        BigDecimal base = BigDecimal.valueOf(precioBase);
        if (Math.abs(factor - 1.0) < 1e-9) {
            return base.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal crudo = base.multiply(BigDecimal.valueOf(factor));
        return crudo.divide(CINCO, 0, RoundingMode.HALF_UP).multiply(CINCO).setScale(2, RoundingMode.HALF_UP);
    }

    private static java.util.Optional<TarifaAplicada> tarifaVigente(List<TarifaAplicada> tarifas, LocalDate noche) {
        return tarifas.stream()
                .filter(t -> t.cubre(noche))
                .max((a, b) -> a.getFechaAprobacion().compareTo(b.getFechaAprobacion()));
    }
}
