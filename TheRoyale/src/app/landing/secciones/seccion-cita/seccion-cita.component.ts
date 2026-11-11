import { Component } from '@angular/core';
import { AparecerDirective } from '../../../shared/aparecer.directive';

// Cita destacada de la landing entre las secciones de hotel y habitaciones. Contenido estático.
@Component({
  selector: 'app-seccion-cita',
  standalone: true,
  imports: [AparecerDirective],
  templateUrl: './seccion-cita.component.html'
})
export class SeccionCitaComponent {}
