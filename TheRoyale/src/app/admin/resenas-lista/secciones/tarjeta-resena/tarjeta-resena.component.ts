import { Component, input, output } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Resena } from '../../../../models/resena.model';
import { EstrellasComponent } from '../../../../shared/estrellas/estrellas.component';

// Tarjeta de una reseña en la moderación del admin: autor, tipo, estadía, estrellas y texto,
// con "Hide" o "Show" según su visibilidad. Solo avisa al padre, que hace la petición.
@Component({
  selector: 'app-tarjeta-resena',
  standalone: true,
  imports: [DatePipe, EstrellasComponent],
  templateUrl: './tarjeta-resena.component.html'
})
export class TarjetaResenaComponent {
  resena = input.required<Resena>();
  // Deshabilita los botones mientras la petición de esta reseña está en curso
  procesando = input<boolean>(false);
  ocultar = output<Resena>();
  mostrar = output<Resena>();
}
