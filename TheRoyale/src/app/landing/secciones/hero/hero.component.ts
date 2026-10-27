import { Component, HostListener } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AparecerDirective } from '../../../shared/aparecer.directive';

// Hero de la landing: imagen a pantalla completa con efecto parallax y llamada a reservar.
@Component({
  selector: 'app-hero',
  standalone: true,
  imports: [AparecerDirective, RouterLink],
  templateUrl: './hero.component.html'
})
export class HeroComponent {

  // Parallax de la imagen: baja hasta 120px mientras se hace scroll
  desplazamiento = 0;

  @HostListener('window:scroll')
  alHacerScroll(): void {
    this.desplazamiento = Math.min((window.scrollY || 0) * 0.13, 120);
  }
}
