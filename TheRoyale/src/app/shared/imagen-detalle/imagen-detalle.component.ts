import { Component, input } from '@angular/core';

// Columna izquierda de las páginas de detalle: foto principal
@Component({
  selector: 'app-imagen-detalle',
  standalone: true,
  templateUrl: './imagen-detalle.component.html'
})
export class ImagenDetalleComponent {
  url = input<string>('');
  alt = input<string>('');
}
