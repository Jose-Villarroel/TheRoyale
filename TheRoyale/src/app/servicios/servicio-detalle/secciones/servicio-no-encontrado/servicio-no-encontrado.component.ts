import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

// Pantalla para una url de servicio que no existe (el servicio-no-encontrado.html del backend)
@Component({
  selector: 'app-servicio-no-encontrado',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './servicio-no-encontrado.component.html'
})
export class ServicioNoEncontradoComponent {}
