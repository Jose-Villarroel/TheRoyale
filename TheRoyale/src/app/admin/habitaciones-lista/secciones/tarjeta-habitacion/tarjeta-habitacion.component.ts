import { Component, input, output } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Habitacion } from '../../../../models/habitacion.model';

// Tarjeta de una habitación. No borra nada: avisa al padre y él decide (confirmación + servicio).
@Component({
  selector: 'app-tarjeta-habitacion',
  standalone: true,
  imports: [RouterLink, CurrencyPipe],
  templateUrl: './tarjeta-habitacion.component.html'
})
export class TarjetaHabitacionComponent {
  habitacion = input.required<Habitacion>();
  // La habitación ya no trae su tipo completo: la imagen la resuelve el padre por tipoHabitacionId
  imagenUrl = input<string | null>(null);
  eliminar = output<Habitacion>();
}
