package com.theroyale.backend.dto;

import com.theroyale.backend.model.Cuenta;
import com.theroyale.backend.model.EstadoCuenta;
import com.theroyale.backend.model.ItemConsumo;
import com.theroyale.backend.model.Pago;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

// ===== La cuenta de una reserva tal como la consume el portal del operador =====
// Hace falta un DTO porque en la entidad Cuenta las listas de consumos y pagos van
// marcadas @JsonIgnore (evitan recursión con ItemConsumo.cuenta y Pago.cuenta),
// y la reserva es LAZY: serializarla directa rompería fuera de la transacción.
public record CuentaResponse(
        Long id,
        Long reservaId,
        EstadoCuenta estado,
        LocalDateTime fechaCreacion,
        List<ItemResponse> items,
        List<PagoResponse> pagos,
        BigDecimal totalConsumos,
        BigDecimal totalPagado,
        BigDecimal saldo
) {

    public record ItemResponse(
            Long id,
            Long servicioId,
            String servicioNombre,
            String operadorNombre,
            Integer cantidad,
            LocalDateTime fechaHora,
            BigDecimal precioUnitario,
            BigDecimal total,
            Boolean pagado
    ) {
    }

    public record PagoResponse(
            Long id,
            BigDecimal monto,
            LocalDateTime fecha,
            String metodoPago,
            String operadorNombre
    ) {
    }

    public static CuentaResponse desde(Cuenta cuenta) {
        return new CuentaResponse(
                cuenta.getId(),
                cuenta.getReserva().getId(),
                cuenta.getEstado(),
                cuenta.getFechaCreacion(),
                cuenta.getItemsConsumo().stream()
                        .sorted(Comparator.comparing(ItemConsumo::getFechaHora))
                        .map(CuentaResponse::desdeItem)
                        .toList(),
                cuenta.getPagos().stream()
                        .sorted(Comparator.comparing(Pago::getFecha))
                        .map(CuentaResponse::desdePago)
                        .toList(),
                cuenta.getTotalConsumos(),
                cuenta.getTotalPagado(),
                cuenta.getSaldo());
    }

    private static ItemResponse desdeItem(ItemConsumo item) {
        return new ItemResponse(
                item.getId(),
                item.getServicio().getId(),
                item.getServicio().getNombre(),
                item.getOperador().getNombre(),
                item.getCantidad(),
                item.getFechaHora(),
                item.getPrecioUnitario(),
                item.getTotal(),
                item.getPagado());
    }

    private static PagoResponse desdePago(Pago pago) {
        return new PagoResponse(
                pago.getId(),
                pago.getMonto(),
                pago.getFecha(),
                pago.getMetodoPago(),
                pago.getOperador() == null ? null : pago.getOperador().getNombre());
    }
}
