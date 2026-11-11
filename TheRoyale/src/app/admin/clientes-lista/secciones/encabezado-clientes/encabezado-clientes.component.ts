import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

// Encabezado del listado admin de clientes: eyebrow, enlace "Back to site" y botón para crear un cliente nuevo.
// Componente de presentación sin estado propio.
@Component({
  selector: 'app-encabezado-clientes',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './encabezado-clientes.component.html'
})
export class EncabezadoClientesComponent {}
