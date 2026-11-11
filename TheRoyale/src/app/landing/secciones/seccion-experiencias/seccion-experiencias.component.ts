import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AparecerDirective } from '../../../shared/aparecer.directive';

// Sección de experiencias de la landing ("New York, Your Way"): planes en la ciudad. Contenido estático.
@Component({
  selector: 'app-seccion-experiencias',
  standalone: true,
  imports: [AparecerDirective, RouterLink],
  templateUrl: './seccion-experiencias.component.html'
})
export class SeccionExperienciasComponent {}
