import { Component, input, output } from '@angular/core';
import { Resena } from '../../../../models/resena.model';
import { TarjetaResenaComponent } from '../tarjeta-resena/tarjeta-resena.component';

// Grid de tarjetas de la moderación de reseñas; muestra un aviso si la lista llega vacía.
// Solo presentación: la página padre carga, filtra y oculta / muestra.
@Component({
  selector: 'app-grid-resenas',
  standalone: true,
  imports: [TarjetaResenaComponent],
  templateUrl: './grid-resenas.component.html'
})
export class GridResenasComponent {
  resenas = input.required<Resena[]>();
  // Id de la reseña cuya petición está en curso (null si ninguna)
  procesandoId = input<number | null>(null);
  ocultar = output<Resena>();
  mostrar = output<Resena>();
}
