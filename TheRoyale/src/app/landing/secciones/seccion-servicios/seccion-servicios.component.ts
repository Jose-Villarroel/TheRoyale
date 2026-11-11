import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Servicio } from '../../../models/servicio.model';
import { AparecerDirective } from '../../../shared/aparecer.directive';

// Sección 03 de la landing ("Services"): tarjetas con los servicios del hotel.
// Recibe la lista ya cargada por la landing; cada tarjeta enlaza a su detalle.
@Component({
  selector: 'app-seccion-servicios',
  standalone: true,
  imports: [CommonModule, RouterLink, AparecerDirective],
  templateUrl: './seccion-servicios.component.html'
})
export class SeccionServiciosComponent {

  @Input() servicios: Servicio[] = [];
}
