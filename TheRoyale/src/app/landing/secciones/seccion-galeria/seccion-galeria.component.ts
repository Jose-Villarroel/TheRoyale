import { Component } from '@angular/core';
import { AparecerDirective } from '../../../shared/aparecer.directive';

// Sección 06 de la landing ("Gallery"): mosaico de fotos del hotel. Contenido estático.
@Component({
  selector: 'app-seccion-galeria',
  standalone: true,
  imports: [AparecerDirective],
  templateUrl: './seccion-galeria.component.html'
})
export class SeccionGaleriaComponent {}
