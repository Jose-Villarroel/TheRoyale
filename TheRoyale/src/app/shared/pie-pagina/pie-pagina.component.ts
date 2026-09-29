import { Component } from '@angular/core';

@Component({
  selector: 'app-pie-pagina',
  standalone: true,
  templateUrl: './pie-pagina.component.html'
})
export class PiePaginaComponent {

  manejarFormularioNewsletter(evento: Event): void {
    evento.preventDefault();
    alert('Thank you for subscribing to The Royale newsletter.');
    (evento.target as HTMLFormElement).reset();
  }
}
