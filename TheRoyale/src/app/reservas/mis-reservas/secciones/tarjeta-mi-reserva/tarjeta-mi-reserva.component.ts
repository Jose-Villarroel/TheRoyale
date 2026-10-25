import { Component, input, output } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { Reserva } from '../../../../models/reserva.model';

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
    const ms = new Date(`${r.fechaFin}T00:00:00`).getTime()
             - new Date(`${r.fechaInicio}T00:00:00`).getTime();
    return Math.round(ms / 86400000);
  }

  get total(): number {
    return this.reserva().precioNocheAcordado * this.noches;
  }

  // El backend solo deja tocar reservas que aún no han empezado y siguen vivas
  get sePuedeGestionar(): boolean {
    const r = this.reserva();
    const empiezaDespuesDeHoy = r.fechaInicio > new Date().toISOString().slice(0, 10);
    return empiezaDespuesDeHoy && (r.estado === 'PENDIENTE' || r.estado === 'CONFIRMADA');
  }
}
