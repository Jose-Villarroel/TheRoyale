import { Component, input, output } from '@angular/core';
import { Habitacion } from '../../../../models/habitacion.model';
import { TarjetaHabitacionComponent } from '../tarjeta-habitacion/tarjeta-habitacion.component';

// Grilla de tarjetas: solo recorre la lista y reenvía hacia arriba la habitación a eliminar
@Component({
  selector: 'app-grid-habitaciones',
  standalone: true,
  imports: [TarjetaHabitacionComponent],
  templateUrl: './grid-habitaciones.component.html'
})
export class GridHabitacionesComponent {
  habitaciones = input.required<Habitacion[]>();
  imagenesPorTipo = input<Record<number, string>>({});
  eliminar = output<Habitacion>();
}
