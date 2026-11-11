import { Component, output } from '@angular/core';

// "Zona de peligro" del perfil: borrar la cuenta del cliente. La confirmación y la
// petición (409 si tiene reservas) las hace la página al recibir (eliminar).
// Selector de atributo: el <div class="perfil-zona-peligro"> mantiene sus dos columnas flex.
@Component({
  selector: 'div[app-zona-peligro]',
  standalone: true,
  templateUrl: './zona-peligro.component.html',
  host: { class: 'perfil-zona-peligro' }
})
export class ZonaPeligroComponent {
  eliminar = output<void>();
}
