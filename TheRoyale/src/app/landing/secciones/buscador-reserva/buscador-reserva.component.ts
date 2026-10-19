import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TipoHabitacion } from '../../../models/tipo-habitacion.model';
import { AparecerDirective } from '../../../shared/aparecer.directive';

// Criterios que la landing pasa a /book como query params
export interface BusquedaReserva {
  fechaInicio: string;
  fechaFin: string;
  huespedes: number;
  tipoId: number | null;
}

@Component({
  selector: 'app-buscador-reserva',
  standalone: true,
  imports: [CommonModule, FormsModule, AparecerDirective],
  templateUrl: './buscador-reserva.component.html'
})
export class BuscadorReservaComponent {

  @Input() tipos: TipoHabitacion[] = [];
  @Output() buscar = new EventEmitter<BusquedaReserva>();

  // La llegada no puede ser anterior a hoy
  readonly hoy = new Date().toISOString().slice(0, 10);

  fechaInicio = '';
  fechaFin = '';
  huespedes = 2;
  tipoId: number | null = null;

  // La salida siempre cae al menos un día después de la llegada
  get minimaSalida(): string {
    if (!this.fechaInicio) {
      return this.hoy;
    }
    const siguiente = new Date(`${this.fechaInicio}T00:00:00`);
    siguiente.setDate(siguiente.getDate() + 1);
    return siguiente.toISOString().slice(0, 10);
  }

  enviar(): void {
    this.buscar.emit({
      fechaInicio: this.fechaInicio,
      fechaFin: this.fechaFin,
      huespedes: this.huespedes,
      tipoId: this.tipoId
    });
  }
}
