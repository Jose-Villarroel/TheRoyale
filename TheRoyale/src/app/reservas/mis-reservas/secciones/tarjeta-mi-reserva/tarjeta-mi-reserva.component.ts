import { Component, input, output } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { Reserva } from '../../../../models/reserva.model';
import { Resena } from '../../../../models/resena.model';
import { EstrellasComponent } from '../../../../shared/estrellas/estrellas.component';
import { noches, puedeCancelar } from '../../../../utils/fechas';

// Tarjeta de reserva vista por su dueño: a diferencia de la del panel de gestión,
// lleva las acciones que el huésped puede ejecutar sobre su propia reserva.
// Si la estadía está FINALIZADA ofrece "Write a review" o, si ya la reseñó, "Edit review".
@Component({
  selector: 'app-tarjeta-mi-reserva',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, EstrellasComponent],
  templateUrl: './tarjeta-mi-reserva.component.html'
})
export class TarjetaMiReservaComponent {
  reserva = input.required<Reserva>();
  // Reseña de esta estadía (la página la busca por reserva.resenaId); null si no tiene
  resena = input<Resena | null>(null);
  modificar = output<Reserva>();
  cancelar = output<Reserva>();
  resenar = output<Reserva>();
  editarResena = output<Reserva>();

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

  // RN-1: solo se reseñan las estadías con check-out hecho
  get sePuedeResenar(): boolean {
    return this.reserva().estado === 'FINALIZADA';
  }
}
