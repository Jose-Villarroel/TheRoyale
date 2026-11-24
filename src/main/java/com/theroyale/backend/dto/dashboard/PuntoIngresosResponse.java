package com.theroyale.backend.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

// ===== Un punto de la serie de ingresos (barras apiladas más línea de cobrado) =====
// habitaciones y servicios se apilan porque suman el facturado; cobrado va como línea aparte
// porque es caja, no facturación: confundirlos haría creer que el hotel ingresó lo que aún no cobra.
// etiqueta es el texto del eje ya formateado en el servidor según la agrupación (día, semana o mes).
public record PuntoIngresosResponse(
        String etiqueta,
        LocalDate fecha,
        BigDecimal habitaciones,
        BigDecimal servicios,
        BigDecimal cobrado
) {
}
