import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

// Encabezado del listado admin de habitaciones: eyebrow, enlace "Back to site" y botón para dar de alta una habitación.
// Componente de presentación sin estado propio.
@Component({
  selector: 'app-encabezado-habitaciones',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './encabezado-habitaciones.component.html'
})
export class EncabezadoHabitacionesComponent {}
