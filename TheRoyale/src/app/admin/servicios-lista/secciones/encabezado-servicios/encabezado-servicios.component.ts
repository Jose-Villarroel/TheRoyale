import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

// Encabezado del listado admin de servicios: eyebrow, enlace "Back to site" y botón para crear un servicio nuevo.
// Componente de presentación sin estado propio.
@Component({
  selector: 'app-encabezado-servicios',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './encabezado-servicios.component.html'
})
export class EncabezadoServiciosComponent {}
