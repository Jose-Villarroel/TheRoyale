import { Component, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Operador } from '../../../../models/operador.model';

// Tarjeta de un operador. No borra nada: avisa al padre y él decide (confirmación + servicio).
@Component({
  selector: 'app-tarjeta-operador',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './tarjeta-operador.component.html'
})
export class TarjetaOperadorComponent {
  operador = input.required<Operador>();
  eliminar = output<Operador>();
}
