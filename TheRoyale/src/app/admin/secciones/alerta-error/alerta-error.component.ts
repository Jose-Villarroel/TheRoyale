import { Component, input } from '@angular/core';

// Mensaje de error de negocio que devuelve el servicio (nombre repetido, tipo con habitaciones...)
@Component({
  selector: 'app-alerta-error',
  standalone: true,
  templateUrl: './alerta-error.component.html'
})
export class AlertaErrorComponent {
  mensaje = input<string | null>(null);
}
