import { Component, input } from '@angular/core';
import { Reserva } from '../../../../models/reserva.model';
import { TarjetaReservaComponent } from '../tarjeta-reserva/tarjeta-reserva.component';

@Component({
  selector: 'app-grid-reservas',
  standalone: true,
  imports: [TarjetaReservaComponent],
  templateUrl: './grid-reservas.component.html'
})
export class GridReservasComponent {
  reservas = input.required<Reserva[]>();
}
