import { Component, input } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';

// Paso 01 de la reserva: llegada, salida y cantidad de huéspedes.
// Pinta los controles del FormGroup que crea la página; la consulta de disponibilidad
// la dispara la página al cambiar los valores (valueChanges), no este componente.
// Selector de atributo: el <section class="reserva-paso"> queda hijo directo del <form>.
@Component({
  selector: 'section[app-paso-fechas]',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './paso-fechas.component.html',
  host: { class: 'reserva-paso' }
})
export class PasoFechasComponent {
  formulario = input.required<FormGroup>();
  // Topes de los <input type="date">: hoy y el día siguiente a la llegada
  hoy = input.required<string>();
  minimaSalida = input.required<string>();

  readonly opcionesHuespedes = [1, 2, 3, 4];
}
