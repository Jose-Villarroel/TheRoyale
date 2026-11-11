import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

// Pie de página común a todas las pantallas: enlaces, contacto y formulario de newsletter.
@Component({
  selector: 'app-pie-pagina',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './pie-pagina.component.html'
})
export class PiePaginaComponent {

  manejarFormularioNewsletter(evento: Event): void {
    evento.preventDefault();
    alert('Thank you for subscribing to The Royale newsletter.');
    (evento.target as HTMLFormElement).reset();
  }
}
