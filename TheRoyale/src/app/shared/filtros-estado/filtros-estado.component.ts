import { Component, input, output } from '@angular/core';
import { EstadoReserva, FILTROS_ESTADO_RESERVA } from '../../models/reserva.model';

// Pestañas de filtro por estado de reserva, compartidas por el listado admin y el del operador.
// Solo presentación: la página padre guarda el estado activo y recarga la lista al recibir (filtrar).
// "variante" elige el estilo: botones del panel admin o las pestañas de templates/operator/reservas.html.
@Component({
  selector: 'app-filtros-estado',
  standalone: true,
  templateUrl: './filtros-estado.component.html'
})
export class FiltrosEstadoComponent {
  estadoActivo = input<EstadoReserva | null>(null);
  variante = input<'admin' | 'operador'>('admin');
  filtrar = output<EstadoReserva | null>();

  readonly filtros = FILTROS_ESTADO_RESERVA;
}
