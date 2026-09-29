import { Component } from '@angular/core';
import { AparecerDirective } from '../../../shared/aparecer.directive';

@Component({
  selector: 'app-seccion-experiencias',
  standalone: true,
  imports: [AparecerDirective],
  templateUrl: './seccion-experiencias.component.html'
})
export class SeccionExperienciasComponent {}
