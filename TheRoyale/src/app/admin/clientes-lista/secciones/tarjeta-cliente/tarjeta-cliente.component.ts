import { Component, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Cliente } from '../../../../models/cliente.model';

// Tarjeta de un cliente. No borra nada: avisa al padre y él decide (confirmación + servicio).
@Component({
  selector: 'app-tarjeta-cliente',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './tarjeta-cliente.component.html'
})
export class TarjetaClienteComponent {
  cliente = input.required<Cliente>();
  eliminar = output<Cliente>();
}
