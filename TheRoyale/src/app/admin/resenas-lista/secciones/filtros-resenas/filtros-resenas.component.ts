import { Component, input, output } from '@angular/core';
import { TipoHabitacion } from '../../../../models/tipo-habitacion.model';

// Selectores de tipo de habitación y calificación de la moderación de reseñas.
// Solo presentación: emite el nuevo valor (null = todos) y la página recarga la lista.
@Component({
  selector: 'app-filtros-resenas',
  standalone: true,
  templateUrl: './filtros-resenas.component.html'
})
export class FiltrosResenasComponent {
  tipos = input<TipoHabitacion[]>([]);
  tipoActivo = input<number | null>(null);
  calificacionActiva = input<number | null>(null);
  cambiarTipo = output<number | null>();
  cambiarCalificacion = output<number | null>();

  readonly calificaciones = [5, 4, 3, 2, 1];

  // El <select> entrega texto: "" significa "todos"
  aNumero(valor: string): number | null {
    return valor === '' ? null : Number(valor);
  }
}
