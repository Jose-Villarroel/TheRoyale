import { Component, input, output } from '@angular/core';
import { EstadoReserva, FILTROS_ESTADO_RESERVA } from '../../models/reserva.model';

// Pestaña de filtro: estado null = "All" (sin filtro)
export interface OpcionFiltro<T extends string> {
  etiqueta: string;
  estado: T | null;
}

// Pestañas de filtro por estado, compartidas por el listado admin y el del operador.
// Solo presentación: la página padre guarda el estado activo y recarga la lista al recibir (filtrar).
// "variante" elige el estilo: botones del panel admin o las pestañas de templates/operator/reservas.html.
// Por defecto muestra los estados de reserva; con [opciones] sirve para otros estados
// (por ejemplo, visible / oculta en la moderación de reseñas).
@Component({
  selector: 'app-filtros-estado',
  standalone: true,
  templateUrl: './filtros-estado.component.html'
})
export class FiltrosEstadoComponent<T extends string = EstadoReserva> {
  estadoActivo = input<T | null>(null);
  variante = input<'admin' | 'operador'>('admin');
  opciones = input<readonly OpcionFiltro<T>[]>(FILTROS_ESTADO_RESERVA as unknown as readonly OpcionFiltro<T>[]);
  filtrar = output<T | null>();
}
