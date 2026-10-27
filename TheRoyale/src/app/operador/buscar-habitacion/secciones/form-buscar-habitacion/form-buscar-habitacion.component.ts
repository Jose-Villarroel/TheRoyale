import { Component, input, model, output } from '@angular/core';
import { FormsModule } from '@angular/forms';

// Paso 01 de "Find Guest": campo del número de habitación con Search / Clear.
// Solo presentación: el número viaja en doble vía con [(numero)] y la búsqueda
// la hace la página al recibir (buscar).
@Component({
  selector: 'app-form-buscar-habitacion',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './form-buscar-habitacion.component.html'
})
export class FormBuscarHabitacionComponent {
  numero = model<string>('');
  buscando = input(false);
  // Hay un resultado en pantalla: se ofrece el botón Clear
  hayResultado = input(false);
  buscar = output<void>();
  limpiar = output<void>();
}
