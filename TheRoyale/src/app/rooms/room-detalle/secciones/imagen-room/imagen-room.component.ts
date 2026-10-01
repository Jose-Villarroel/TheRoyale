import { Component, input } from '@angular/core';

// Columna izquierda: foto principal del tipo de habitación
@Component({
  selector: 'app-imagen-room',
  standalone: true,
  templateUrl: './imagen-room.component.html'
})
export class ImagenRoomComponent {
  url = input<string>('');
  alt = input<string>('');
}
