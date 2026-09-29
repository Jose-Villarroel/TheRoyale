import { Component } from '@angular/core';
import { AparecerDirective } from '../../../shared/aparecer.directive';

@Component({
  selector: 'app-seccion-business',
  standalone: true,
  imports: [AparecerDirective],
  templateUrl: './seccion-business.component.html'
})
export class SeccionBusinessComponent {}
