import { Component, input } from '@angular/core';

// Encabezado oscuro de las páginas admin (el hero-interior de Thymeleaf)
@Component({
  selector: 'app-hero-admin',
  standalone: true,
  templateUrl: './hero-admin.component.html'
})
export class HeroAdminComponent {
  titulo = input.required<string>();
  subtitulo = input<string>('');
}
