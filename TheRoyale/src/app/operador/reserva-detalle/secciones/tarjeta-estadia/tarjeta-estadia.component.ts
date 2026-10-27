import { Component, input } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { Reserva } from '../../../../models/reserva.model';

// Primera tarjeta de la columna lateral del detalle: estado, fechas, noches y total de la estadía.
// Las acciones (confirmar, check-in...) se proyectan con <ng-content> desde la página,
// igual que en operator/reserva-detalle.html, donde van dentro de esta misma tarjeta.
// Selector de atributo: el <section> debe ser hijo directo de .operador-lateral.
@Component({
  selector: 'section[app-tarjeta-estadia]',
  standalone: true,
  imports: [CurrencyPipe, DatePipe],
  templateUrl: './tarjeta-estadia.component.html',
  host: { class: 'operador-tarjeta' }
})
export class TarjetaEstadiaComponent {
  reserva = input.required<Reserva>();
  noches = input.required<number>();
  totalEstadia = input.required<number>();
}
