import { Component, input } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { TipoHabitacion } from '../../../../models/tipo-habitacion.model';

// Columna derecha: nombre, descripción, precio "desde" y resumen de disponibilidad
@Component({
  selector: 'app-info-room',
  standalone: true,
  imports: [DecimalPipe, RouterLink],
  templateUrl: './info-room.component.html'
})
export class InfoRoomComponent {
  tipo = input.required<TipoHabitacion>();
  precioDesde = input<number | null>(null);
  disponibles = input<number>(0);
  total = input<number>(0);
}
