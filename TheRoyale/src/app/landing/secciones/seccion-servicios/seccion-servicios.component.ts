import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Servicio } from '../../../models/servicio.model';
import { AparecerDirective } from '../../../shared/aparecer.directive';

@Component({
  selector: 'app-seccion-servicios',
  standalone: true,
  imports: [CommonModule, RouterLink, AparecerDirective],
  templateUrl: './seccion-servicios.component.html'
})
export class SeccionServiciosComponent {

  @Input() servicios: Servicio[] = [];
}
