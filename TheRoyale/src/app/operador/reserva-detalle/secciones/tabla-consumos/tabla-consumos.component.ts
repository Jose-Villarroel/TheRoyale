import { Component, input, output } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { ItemConsumo } from '../../../../models/cuenta.model';

// Servicios cargados a la cuenta de la estadía (AC24). Solo una cuenta abierta deja
// quitar cargos no pagados; la confirmación y la petición las hace la página (quitar).
@Component({
  selector: 'app-tabla-consumos',
  standalone: true,
  imports: [CurrencyPipe, DatePipe],
  templateUrl: './tabla-consumos.component.html'
})
export class TablaConsumosComponent {
  items = input.required<ItemConsumo[]>();
  operable = input(false);
  quitar = output<number>();
}
