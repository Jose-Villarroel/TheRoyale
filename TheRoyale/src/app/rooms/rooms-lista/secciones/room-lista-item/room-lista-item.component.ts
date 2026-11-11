import { Component, input } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { TipoHabitacion } from '../../../../models/tipo-habitacion.model';
import { EstrellasComponent } from '../../../../shared/estrellas/estrellas.component';

// Fila pública de un tipo de habitación (el <article> de rooms-lista.html).
// Selector de atributo sobre el <article> para que sea hijo directo de .rooms-lista:
// así sigue funcionando el :nth-child(even) que alterna la imagen a la derecha.
@Component({
  selector: 'article[app-room-lista-item]',
  standalone: true,
  imports: [DecimalPipe, RouterLink, EstrellasComponent],
  templateUrl: './room-lista-item.component.html'
})
export class RoomListaItemComponent {
  tipo = input.required<TipoHabitacion>();
  posicion = input.required<number>();
  // null = el tipo no tiene habitaciones con precio ("Upon request")
  precioDesde = input<number | null>(null);
  disponibles = input<number>(0);
  // Resumen de reseñas visibles que rooms-lista carga una sola vez con resumenTodos()
  promedio = input<number | null>(null);
  totalResenas = input<number>(0);
}
