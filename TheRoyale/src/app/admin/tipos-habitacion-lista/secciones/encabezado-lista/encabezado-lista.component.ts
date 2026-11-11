import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

// Encabezado del listado admin de tipos de habitación: eyebrow, enlace "Back to site" y botón para crear un tipo nuevo.
// Componente de presentación sin estado propio.
@Component({
  selector: 'app-encabezado-lista',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './encabezado-lista.component.html'
})
export class EncabezadoListaComponent {}
