import { Component, input } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Reserva } from '../../../../models/reserva.model';
import { noches } from '../../../../utils/fechas';

// Tabla de reservas del portal del operador (la .tabla-operador de templates/operator/reservas.html).
// Solo presentación: recibe la lista ya filtrada y enlaza cada fila con su detalle.
@Component({
  selector: 'app-tabla-reservas-operador',
  standalone: true,
  imports: [DatePipe, RouterLink],
  templateUrl: './tabla-reservas-operador.component.html'
})
export class TablaReservasOperadorComponent {
  reservas = input.required<Reserva[]>();

  // Noches de la estadía, para la línea secundaria de la columna "Stay"
  noches(reserva: Reserva): number {
    return noches(reserva.fechaInicio, reserva.fechaFin);
  }
}
