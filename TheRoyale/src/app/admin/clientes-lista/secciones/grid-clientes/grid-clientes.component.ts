import { Component, input, output } from '@angular/core';
import { Cliente } from '../../../../models/cliente.model';
import { TarjetaClienteComponent } from '../tarjeta-cliente/tarjeta-cliente.component';

// Grilla de tarjetas: solo recorre la lista y reenvía hacia arriba el cliente a eliminar
@Component({
  selector: 'app-grid-clientes',
  standalone: true,
  imports: [TarjetaClienteComponent],
  templateUrl: './grid-clientes.component.html'
})
export class GridClientesComponent {
  clientes = input.required<Cliente[]>();
  eliminar = output<Cliente>();
}
