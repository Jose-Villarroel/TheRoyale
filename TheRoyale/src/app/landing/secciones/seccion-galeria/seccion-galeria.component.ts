import { Component } from '@angular/core';
import { AparecerDirective } from '../../../shared/aparecer.directive';

@Component({
  selector: 'app-seccion-galeria',
  standalone: true,
  imports: [AparecerDirective],
  templateUrl: './seccion-galeria.component.html'
})
export class SeccionGaleriaComponent {}
