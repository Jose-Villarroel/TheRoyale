import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AparecerDirective } from '../../../shared/aparecer.directive';

// Llamada final a la acción de la landing ("Your New York Story Starts Here") con enlace a reservar.
@Component({
  selector: 'app-seccion-cta',
  standalone: true,
  imports: [AparecerDirective, RouterLink],
  templateUrl: './seccion-cta.component.html'
})
export class SeccionCtaComponent {}
