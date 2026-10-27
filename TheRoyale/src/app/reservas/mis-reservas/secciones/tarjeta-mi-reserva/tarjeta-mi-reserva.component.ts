import { Component, input, output } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { Reserva } from '../../../../models/reserva.model';
import { noches, puedeCancelar } from '../../../../utils/fechas';

// Tarjeta de reserva vista por su dueño: a diferencia de la del panel de gestión,
// lleva las acciones que el huésped puede ejecutar sobre su propia reserva.
@Component({
  selector: 'app-tarjeta-mi-reserva',
  standalone: true,
  imports: [CurrencyPipe, DatePipe],
  templateUrl: './tarjeta-mi-reserva.component.html'
})
export class TarjetaMiReservaComponent {
  reserva = input.required<Reserva>();
  modificar = output<Reserva>();
  cancelar = output<Reserva>();

  get noches(): number {
    const r = this.reserva();
    return noches(r.fechaInicio, r.fechaFin);
  }

  get total(): number {
    return this.reserva().precioNocheAcordado * this.noches;
  }

  // El backend solo deja tocar reservas que aún no han empezado y siguen vivas
  get sePuedeGestionar(): boolean {
    return puedeCancelar(this.reserva());
  }
}
