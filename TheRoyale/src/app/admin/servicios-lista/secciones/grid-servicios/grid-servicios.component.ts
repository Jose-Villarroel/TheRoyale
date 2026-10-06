import { Component, input, output } from '@angular/core';
import { Servicio } from '../../../../models/servicio.model';
import { TarjetaServicioComponent } from '../tarjeta-servicio/tarjeta-servicio.component';

// Grilla de tarjetas: solo recorre la lista y reenvía hacia arriba el servicio a eliminar
@Component({
  selector: 'app-grid-servicios',
  standalone: true,
  imports: [TarjetaServicioComponent],
  templateUrl: './grid-servicios.component.html'
})
export class GridServiciosComponent {
  servicios = input.required<Servicio[]>();
  eliminar = output<Servicio>();
}
