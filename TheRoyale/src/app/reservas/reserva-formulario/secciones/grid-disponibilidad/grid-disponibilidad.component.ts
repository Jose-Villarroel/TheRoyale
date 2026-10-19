import { Component, input, output } from '@angular/core';
import { DisponibilidadTipo } from '../../../../models/reserva.model';
import { TarjetaDisponibilidadComponent } from '../tarjeta-disponibilidad/tarjeta-disponibilidad.component';

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
