import { Component } from '@angular/core';
import { AparecerDirective } from '../../../shared/aparecer.directive';

@Component({
  selector: 'app-seccion-cita',
  standalone: true,
  imports: [AparecerDirective],
  templateUrl: './seccion-cita.component.html'
})
export class SeccionCitaComponent {}
