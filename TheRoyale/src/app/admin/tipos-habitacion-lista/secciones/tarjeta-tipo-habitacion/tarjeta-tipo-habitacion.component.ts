import { Component, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TipoHabitacion } from '../../../../models/tipo-habitacion.model';

// Tarjeta de un solo tipo. No borra nada: avisa al padre y él decide (confirmación + servicio).
@Component({
  selector: 'app-tarjeta-tipo-habitacion',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './tarjeta-tipo-habitacion.component.html'
})
export class TarjetaTipoHabitacionComponent {
  tipo = input.required<TipoHabitacion>();
  eliminar = output<TipoHabitacion>();
}
