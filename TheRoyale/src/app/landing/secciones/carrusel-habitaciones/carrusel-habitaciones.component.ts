import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { TipoHabitacion } from '../../../models/tipo-habitacion.model';
import { AparecerDirective } from '../../../shared/aparecer.directive';

@Component({
  selector: 'app-carrusel-habitaciones',
  standalone: true,
  imports: [CommonModule, RouterLink, AparecerDirective],
  templateUrl: './carrusel-habitaciones.component.html'
})
export class CarruselHabitacionesComponent {

  @Input() tipos: TipoHabitacion[] = [];

  // Slide visible. La plantilla lo usa para desplazar el track y la barra de progreso.
  indice = 0;

  // Contador "01 / 05"
  get contador(): string {
    const total = this.tipos.length;
    if (total === 0) {
      return '00 / 00';
    }
    const actual = String(this.indice + 1).padStart(2, '0');
    return `${actual} / ${String(total).padStart(2, '0')}`;
  }

  // Ancho en % de la barra de progreso: un tramo por cada tipo de habitación
  get anchoProgreso(): number {
    return this.tipos.length === 0 ? 0 : 100 / this.tipos.length;
  }

  irHabitacionAnterior(): void {
    const total = this.tipos.length;
    if (total === 0) return;
    this.indice = (this.indice - 1 + total) % total;
  }

  irHabitacionSiguiente(): void {
    const total = this.tipos.length;
    if (total === 0) return;
    this.indice = (this.indice + 1) % total;
  }
}
