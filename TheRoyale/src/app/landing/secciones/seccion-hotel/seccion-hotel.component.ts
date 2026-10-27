import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AparecerDirective } from '../../../shared/aparecer.directive';

// Sección 01 de la landing ("The Hotel"): presentación del hotel con texto e imagen.
@Component({
  selector: 'app-seccion-hotel',
  standalone: true,
  imports: [AparecerDirective, RouterLink],
  templateUrl: './seccion-hotel.component.html'
})
export class SeccionHotelComponent {}
