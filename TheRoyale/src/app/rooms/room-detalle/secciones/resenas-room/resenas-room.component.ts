import { Component, computed, input, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { EstrellasComponent } from '../../../../shared/estrellas/estrellas.component';
import { Resena, ResumenResenas } from '../../../../models/resena.model';

// Sección de opiniones del tipo de habitación, debajo de la disponibilidad: promedio grande,
// total, barras de distribución 5→1 y la lista de reseñas visibles (las 5 más recientes y
// "Show all"). Solo presentación: la página padre carga el resumen y las reseñas.
@Component({
  selector: 'app-resenas-room',
  standalone: true,
  imports: [DatePipe, DecimalPipe, EstrellasComponent],
  templateUrl: './resenas-room.component.html'
})
export class ResenasRoomComponent {
  resumen = input<ResumenResenas | null>(null);
  // El backend ya las entrega de la más reciente a la más antigua
  resenas = input<Resena[]>([]);

  static readonly VISIBLES_INICIALES = 5;
  readonly niveles = [5, 4, 3, 2, 1];
  readonly verTodas = signal(false);

  readonly total = computed(() => this.resumen()?.total ?? this.resenas().length);

  readonly resenasMostradas = computed(() =>
    this.verTodas() ? this.resenas() : this.resenas().slice(0, ResenasRoomComponent.VISIBLES_INICIALES));

  readonly hayMas = computed(() =>
    !this.verTodas() && this.resenas().length > ResenasRoomComponent.VISIBLES_INICIALES);

  cantidad(nivel: number): number {
    return this.resumen()?.distribucion?.[nivel] ?? 0;
  }

  // Ancho de la barra en %, sobre el total de reseñas visibles
  porcentaje(nivel: number): number {
    const total = this.total();
    return total > 0 ? Math.round((this.cantidad(nivel) / total) * 100) : 0;
  }
}
