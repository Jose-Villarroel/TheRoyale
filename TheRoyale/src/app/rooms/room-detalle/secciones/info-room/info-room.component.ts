import { Component, input } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { TipoHabitacion } from '../../../../models/tipo-habitacion.model';
import { EstrellasComponent } from '../../../../shared/estrellas/estrellas.component';

// Columna derecha: nombre, descripción, precio "desde", promedio de reseñas y resumen de disponibilidad
@Component({
  selector: 'app-info-room',
  standalone: true,
  imports: [DecimalPipe, RouterLink, EstrellasComponent],
  templateUrl: './info-room.component.html'
})
export class InfoRoomComponent {
  tipo = input.required<TipoHabitacion>();
  precioDesde = input<number | null>(null);
  disponibles = input<number>(0);
  total = input<number>(0);
  // Resumen de reseñas visibles: promedio null y 0 reseñas si el tipo aún no tiene
  promedio = input<number | null>(null);
  totalResenas = input<number>(0);
}
