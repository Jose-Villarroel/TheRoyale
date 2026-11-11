import { Component, input, output } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { DisponibilidadTipo } from '../../../../models/reserva.model';

// Tarjeta de un tipo de habitación dentro del paso 02 del formulario de reserva.
@Component({
  selector: 'app-tarjeta-disponibilidad',
  standalone: true,
  imports: [CurrencyPipe],
  templateUrl: './tarjeta-disponibilidad.component.html'
})
export class TarjetaDisponibilidadComponent {
  disponibilidad = input.required<DisponibilidadTipo>();
  seleccionada = input<boolean>(false);
  elegir = output<DisponibilidadTipo>();

  get agotada(): boolean {
    return this.disponibilidad().habitacionesDisponibles === 0;
  }

  seleccionar(): void {
    if (!this.agotada) {
      this.elegir.emit(this.disponibilidad());
    }
  }
}
