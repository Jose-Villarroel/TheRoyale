import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TipoHabitacion } from '../../../models/tipo-habitacion.model';
import { AparecerDirective } from '../../../shared/aparecer.directive';

@Component({
  selector: 'app-buscador-reserva',
  standalone: true,
  imports: [CommonModule, AparecerDirective],
  templateUrl: './buscador-reserva.component.html'
})
export class BuscadorReservaComponent {

  @Input() tipos: TipoHabitacion[] = [];
  @Output() buscar = new EventEmitter<Event>();
}
