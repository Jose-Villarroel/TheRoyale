import { Component, input } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { Servicio } from '../../../../models/servicio.model';

// Tarjeta pública de un servicio. Selector de atributo sobre el <a> para que la
// propia tarjeta sea el hijo de la grilla: con un elemento envoltorio de más,
// .tarjeta-servicio-item dejaría de estirarse a la altura de la celda.
@Component({
  selector: 'a[app-tarjeta-servicio-item]',
  standalone: true,
  imports: [DecimalPipe],
  templateUrl: './tarjeta-servicio-item.component.html'
})
export class TarjetaServicioItemComponent {
  servicio = input.required<Servicio>();
  posicion = input.required<number>();
}
