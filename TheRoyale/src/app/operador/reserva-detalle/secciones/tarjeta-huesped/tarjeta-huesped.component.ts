import { Component, input } from '@angular/core';
import { Reserva } from '../../../../models/reserva.model';

// Tarjeta con los datos del huésped de la reserva (AC22). La cédula se muestra para que
// el operador confirme que atiende a la persona correcta.
// Selector de atributo: el <section> debe ser hijo directo de .operador-lateral.
@Component({
  selector: 'section[app-tarjeta-huesped]',
  standalone: true,
  templateUrl: './tarjeta-huesped.component.html',
  host: { class: 'operador-tarjeta' }
})
export class TarjetaHuespedComponent {
  reserva = input.required<Reserva>();
}
