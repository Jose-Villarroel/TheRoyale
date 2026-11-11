import { Component, input } from '@angular/core';

// Galería de fotos al pie del detalle del servicio
@Component({
  selector: 'app-galeria-servicio',
  standalone: true,
  templateUrl: './galeria-servicio.component.html'
})
export class GaleriaServicioComponent {
  urls = input.required<string[]>();
  nombre = input<string>('');
}
