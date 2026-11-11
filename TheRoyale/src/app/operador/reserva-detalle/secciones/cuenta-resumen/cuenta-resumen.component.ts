import { Component, input } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { Cuenta } from '../../../../models/cuenta.model';

// Cabecera de la cuenta de la estadía: estado, totales (cargos, pagado, saldo) y el aviso
// de "Account settled". Sin cuenta (reserva aún PENDIENTE) muestra el estado vacío.
@Component({
  selector: 'app-cuenta-resumen',
  standalone: true,
  imports: [CurrencyPipe],
  templateUrl: './cuenta-resumen.component.html'
})
export class CuentaResumenComponent {
  cuenta = input<Cuenta | null>(null);
  numeroHabitacion = input.required<string>();
  // Saldo en cero tras haber pagado algo (lo calcula la página)
  saldada = input(false);
}
