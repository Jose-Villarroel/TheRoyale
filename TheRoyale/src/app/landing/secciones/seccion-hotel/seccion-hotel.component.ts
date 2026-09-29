import { Component } from '@angular/core';
import { AparecerDirective } from '../../../shared/aparecer.directive';

@Component({
  selector: 'app-seccion-hotel',
  standalone: true,
  imports: [AparecerDirective],
  templateUrl: './seccion-hotel.component.html'
})
export class SeccionHotelComponent {}
