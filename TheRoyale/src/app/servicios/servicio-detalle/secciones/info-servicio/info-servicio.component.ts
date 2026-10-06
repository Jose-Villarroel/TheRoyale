import { Component, input } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Servicio } from '../../../../models/servicio.model';
import { FeaturesServicioComponent } from '../features-servicio/features-servicio.component';

// Columna derecha del detalle: nombre, descripción, precio, qué incluye y acciones
@Component({
  selector: 'app-info-servicio',
  standalone: true,
  imports: [DecimalPipe, RouterLink, FeaturesServicioComponent],
  templateUrl: './info-servicio.component.html'
})
export class InfoServicioComponent {
  servicio = input.required<Servicio>();
}
