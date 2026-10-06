import { Component, input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

// Barra "View as": alterna entre la vista de tabla y la de tarjetas.
// routerLinkActive marca la vista actual, así que no hace falta pasarla por input.
@Component({
  selector: 'app-barra-vistas',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './barra-vistas.component.html'
})
export class BarraVistasComponent {
  cantidad = input.required<number>();
}
