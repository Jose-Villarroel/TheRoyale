import { Component, input } from '@angular/core';
import { Reserva } from '../../../../models/reserva.model';
import { TarjetaReservaComponent } from '../tarjeta-reserva/tarjeta-reserva.component';

// Grid de tarjetas del listado admin de reservas; muestra un aviso si la lista llega vacía.
// Solo presentación: la página padre carga y filtra las reservas.
@Component({
  selector: 'app-grid-reservas',
  standalone: true,
  imports: [TarjetaReservaComponent],
  templateUrl: './grid-reservas.component.html'
})
export class GridReservasComponent {
  reservas = input.required<Reserva[]>();
}
