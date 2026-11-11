import { Component, input, output } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Reserva } from '../../../../models/reserva.model';

// Paso 02 de "Find Guest": huésped y reserva activa de la habitación buscada, para que el
// operador confirme la identidad (nombre y cédula) antes de gestionar la cuenta.
// Solo presentación: (gestionar) lleva al detalle y (limpiar) reinicia la búsqueda en la página.
@Component({
  selector: 'app-tarjeta-cliente-encontrado',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './tarjeta-cliente-encontrado.component.html'
})
export class TarjetaClienteEncontradoComponent {
  reserva = input.required<Reserva>();
  gestionar = output<void>();
  limpiar = output<void>();
}
