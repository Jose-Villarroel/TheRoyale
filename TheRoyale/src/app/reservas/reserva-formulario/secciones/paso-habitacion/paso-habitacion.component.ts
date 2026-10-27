import { Component, input, output } from '@angular/core';
import { DisponibilidadTipo } from '../../../../models/reserva.model';
import { GridDisponibilidadComponent } from '../grid-disponibilidad/grid-disponibilidad.component';

// Paso 02 de la reserva: tipos de habitación disponibles para las fechas elegidas.
// Mientras no haya un rango válido, o la consulta siga en curso, muestra el aviso que toca.
// Selector de atributo: el <section class="reserva-paso"> queda hijo directo del <form>.
@Component({
  selector: 'section[app-paso-habitacion]',
  standalone: true,
  imports: [GridDisponibilidadComponent],
  templateUrl: './paso-habitacion.component.html',
  host: { class: 'reserva-paso' }
})
export class PasoHabitacionComponent {
  rangoValido = input.required<boolean>();
  consultando = input(false);
  noches = input(0);
  disponibilidad = input.required<DisponibilidadTipo[]>();
  tipoSeleccionadoId = input<number | null>(null);
  elegir = output<DisponibilidadTipo>();
}
