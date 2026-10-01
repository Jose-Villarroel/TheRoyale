import { Component, input, output } from '@angular/core';
import { TipoHabitacion } from '../../../../models/tipo-habitacion.model';
import { TarjetaTipoHabitacionComponent } from '../tarjeta-tipo-habitacion/tarjeta-tipo-habitacion.component';

// Grilla de tarjetas: solo recorre la lista y reenvía hacia arriba el tipo a eliminar
@Component({
  selector: 'app-grid-tipos-habitacion',
  standalone: true,
  imports: [TarjetaTipoHabitacionComponent],
  templateUrl: './grid-tipos-habitacion.component.html'
})
export class GridTiposHabitacionComponent {
  tipos = input.required<TipoHabitacion[]>();
  eliminar = output<TipoHabitacion>();
}
