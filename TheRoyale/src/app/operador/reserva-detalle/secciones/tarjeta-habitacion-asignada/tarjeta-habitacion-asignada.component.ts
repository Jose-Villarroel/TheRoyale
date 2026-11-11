import { Component, input } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { Reserva } from '../../../../models/reserva.model';

// Habitación asignada a la reserva: foto del tipo, número y tarifa acordada por noche.
// Selector de atributo: el <section> debe ser hijo directo de .operador-lateral.
@Component({
  selector: 'section[app-tarjeta-habitacion-asignada]',
  standalone: true,
  imports: [CurrencyPipe],
  templateUrl: './tarjeta-habitacion-asignada.component.html',
  host: { class: 'operador-tarjeta operador-habitacion' }
})
export class TarjetaHabitacionAsignadaComponent {
  reserva = input.required<Reserva>();
}
