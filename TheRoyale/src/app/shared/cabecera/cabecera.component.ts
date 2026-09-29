import { Component, EventEmitter, HostListener, Output } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-cabecera',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './cabecera.component.html'
})
export class CabeceraComponent {

  // El menú móvil es hermano de la cabecera, así que quien la usa decide cuándo abrirlo.
  @Output() abrirMenu = new EventEmitter<void>();

  // Cabecera sólida una vez que se bajan 90px, igual que antes pero con binding en vez de classList
  solida = false;

  @HostListener('window:scroll')
  alHacerScroll(): void {
    this.solida = (window.scrollY || 0) > 90;
  }
}
