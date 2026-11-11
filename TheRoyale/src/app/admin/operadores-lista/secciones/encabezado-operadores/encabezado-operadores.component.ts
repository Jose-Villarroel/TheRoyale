import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

// Encabezado del listado admin de operadores: eyebrow, enlace "Back to site" y botón para registrar un operador nuevo.
// Componente de presentación sin estado propio.
@Component({
  selector: 'app-encabezado-operadores',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './encabezado-operadores.component.html'
})
export class EncabezadoOperadoresComponent {
}
