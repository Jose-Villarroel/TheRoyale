import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AparecerDirective } from '../../../shared/aparecer.directive';

// Sección 05 de la landing ("Business Travel"): espacios y servicios para viajes de negocios.
// Contenido estático, igual que en landing.html de Thymeleaf.
@Component({
  selector: 'app-seccion-business',
  standalone: true,
  imports: [AparecerDirective, RouterLink],
  templateUrl: './seccion-business.component.html'
})
export class SeccionBusinessComponent {}
