import { Component, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Servicio } from '../../../../models/servicio.model';

// Tarjeta de un servicio en el listado admin: muestra los datos y avisa hacia arriba del borrado
@Component({
  selector: 'app-tarjeta-servicio',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './tarjeta-servicio.component.html'
})
export class TarjetaServicioComponent {
  servicio = input.required<Servicio>();
  eliminar = output<Servicio>();
}
