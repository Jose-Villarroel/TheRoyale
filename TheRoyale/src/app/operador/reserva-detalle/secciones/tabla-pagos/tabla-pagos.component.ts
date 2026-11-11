import { Component, input } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { Pago } from '../../../../models/cuenta.model';

// Pagos registrados contra la cuenta de la estadía (AC25). Solo lectura.
@Component({
  selector: 'app-tabla-pagos',
  standalone: true,
  imports: [CurrencyPipe, DatePipe],
  templateUrl: './tabla-pagos.component.html'
})
export class TablaPagosComponent {
  pagos = input.required<Pago[]>();
}
