import { Component, input } from '@angular/core';

// Banda oscura de cierre de las páginas interiores.
// El texto por defecto es el del detalle de habitación; servicios pasa el suyo.
@Component({
  selector: 'app-cta-interior',
  standalone: true,
  templateUrl: './cta-interior.component.html'
})
export class CtaInteriorComponent {
  texto = input<string>('Make New York feel entirely yours.');
}
