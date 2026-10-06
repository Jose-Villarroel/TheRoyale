import { Component, input } from '@angular/core';

// Encabezado oscuro de las páginas interiores (el hero-interior de Thymeleaf).
// El eyebrow por defecto es "Admin" porque es el que usan las páginas del panel;
// las páginas públicas pasan el suyo (ej. "03 — Services").
@Component({
  selector: 'app-hero-interior',
  standalone: true,
  templateUrl: './hero-interior.component.html'
})
export class HeroInteriorComponent {
  eyebrow = input<string>('Admin');
  titulo = input.required<string>();
  subtitulo = input<string>('');
}
