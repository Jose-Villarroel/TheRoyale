import { Component, input } from '@angular/core';

// Bloque "What's included" del detalle del servicio
@Component({
  selector: 'app-features-servicio',
  standalone: true,
  templateUrl: './features-servicio.component.html'
})
export class FeaturesServicioComponent {
  caracteristicas = input.required<string[]>();
}
