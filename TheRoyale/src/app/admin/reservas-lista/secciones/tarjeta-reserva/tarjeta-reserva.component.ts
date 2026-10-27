import { Component, input } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Reserva } from '../../../../models/reserva.model';
import { noches } from '../../../../utils/fechas';

@Component({
  selector: 'app-tarjeta-reserva',
  standalone: true,
  imports: [DatePipe, RouterLink],
  templateUrl: './tarjeta-reserva.component.html'
})
export class TarjetaReservaComponent {
  reserva = input.required<Reserva>();

  // El backend expone getNoches(), pero se recalcula por si la respuesta no lo trae
  get noches(): number {
    const r = this.reserva();
    return noches(r.fechaInicio, r.fechaFin);
  }
}
