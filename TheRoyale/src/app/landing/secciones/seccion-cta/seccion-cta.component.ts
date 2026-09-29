import { Component } from '@angular/core';
import { AparecerDirective } from '../../../shared/aparecer.directive';

@Component({
  selector: 'app-seccion-cta',
  standalone: true,
  imports: [AparecerDirective],
  templateUrl: './seccion-cta.component.html'
})
export class SeccionCtaComponent {}
