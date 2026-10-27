import { Component, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';

// Botones del ciclo de vida de la reserva: confirmar, check-in, check-out, cancelar y eliminar.
// Solo presentación: la página decide qué transiciones aplican (inputs puede*) y
// ejecuta la petición al recibir el evento correspondiente.
@Component({
  selector: 'app-acciones-reserva',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './acciones-reserva.component.html'
})
export class AccionesReservaComponent {
  puedeConfirmar = input(false);
  puedeCheckIn = input(false);
  puedeCheckOut = input(false);
  puedeCancelar = input(false);
  puedeEliminar = input(false);

  confirmar = output<void>();
  checkIn = output<void>();
  checkOut = output<void>();
  cancelar = output<void>();
  eliminar = output<void>();
}
