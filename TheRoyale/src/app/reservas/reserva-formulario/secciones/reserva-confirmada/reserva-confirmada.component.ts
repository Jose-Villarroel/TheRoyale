import { Component, input, output } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Reserva } from '../../../../models/reserva.model';

// Cierre del flujo de reserva: todo lo que el huésped necesita saber de la reserva recién
// creada (o modificada), sin pasar por ningún listado. (reservarOtra) reinicia el formulario.
@Component({
  selector: 'app-reserva-confirmada',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, RouterLink],
  templateUrl: './reserva-confirmada.component.html'
})
export class ReservaConfirmadaComponent {
  reserva = input.required<Reserva>();
  enEdicion = input(false);
  reservarOtra = output<void>();
}
