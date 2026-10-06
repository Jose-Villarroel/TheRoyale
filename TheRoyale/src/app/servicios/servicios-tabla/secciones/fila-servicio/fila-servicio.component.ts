import { Component, input } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Servicio } from '../../../../models/servicio.model';

// Fila de la tabla de servicios. Selector de atributo porque dentro de un <tbody>
// solo puede haber <tr>: un <app-...> ahí rompería el HTML.
@Component({
  selector: 'tr[app-fila-servicio]',
  standalone: true,
  imports: [DecimalPipe, RouterLink],
  templateUrl: './fila-servicio.component.html'
})
export class FilaServicioComponent {
  servicio = input.required<Servicio>();
  posicion = input.required<number>();
}
