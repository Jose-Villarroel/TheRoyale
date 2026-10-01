import { Component, input } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { Habitacion } from '../../../../models/habitacion.model';

// Lista de habitaciones de la categoría con su estado y precio por noche
@Component({
  selector: 'app-disponibilidad-room',
  standalone: true,
  imports: [DecimalPipe],
  templateUrl: './disponibilidad-room.component.html'
})
export class DisponibilidadRoomComponent {
  habitaciones = input.required<Habitacion[]>();
}
