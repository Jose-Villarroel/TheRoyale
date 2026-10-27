import { Component, input, output } from '@angular/core';
import { DisponibilidadTipo } from '../../../../models/reserva.model';
import { TarjetaDisponibilidadComponent } from '../tarjeta-disponibilidad/tarjeta-disponibilidad.component';

// Grid de tarjetas de disponibilidad por tipo de habitación (paso 02 de la reserva).
// Marca la tarjeta elegida y reenvía (elegir) a la página.
@Component({
  selector: 'app-grid-disponibilidad',
  standalone: true,
  imports: [TarjetaDisponibilidadComponent],
  templateUrl: './grid-disponibilidad.component.html'
})
export class GridDisponibilidadComponent {
  disponibilidad = input.required<DisponibilidadTipo[]>();
  tipoSeleccionadoId = input<number | null>(null);
  elegir = output<DisponibilidadTipo>();
}
