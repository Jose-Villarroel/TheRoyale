import { Component, input } from '@angular/core';

// Previa de la imagen: se actualiza sola con el valor del control, sin el script de Thymeleaf
@Component({
  selector: 'app-imagen-previa',
  standalone: true,
  templateUrl: './imagen-previa.component.html'
})
export class ImagenPreviaComponent {
  url = input<string | null>('');
}
