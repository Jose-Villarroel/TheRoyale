import { Component, input, output } from '@angular/core';
import { Operador } from '../../../../models/operador.model';
import { TarjetaOperadorComponent } from '../tarjeta-operador/tarjeta-operador.component';

// Grilla de tarjetas: solo recorre la lista y reenvía hacia arriba el operador a eliminar
@Component({
  selector: 'app-grid-operadores',
  standalone: true,
  imports: [TarjetaOperadorComponent],
  templateUrl: './grid-operadores.component.html'
})
export class GridOperadoresComponent {
  operadores = input.required<Operador[]>();
  eliminar = output<Operador>();
}
