import { Component, input } from '@angular/core';
import { AbstractControl } from '@angular/forms';

// Muestra los mensajes de validación de un control. "mensajes" asocia cada error
// (required, minlength...) con su texto; solo se pintan los que el control tiene activos.
@Component({
  selector: 'app-campo-error',
  standalone: true,
  templateUrl: './campo-error.component.html'
})
export class CampoErrorComponent {
  control = input.required<AbstractControl | null>();
  mensajes = input.required<Record<string, string>>();
  // Por defecto espera a que el usuario toque el campo; en false se muestra apenas sea inválido
  soloSiTocado = input<boolean>(true);

  get erroresVisibles(): string[] {
    const control = this.control();
    if (!control || !control.invalid || (this.soloSiTocado() && !control.touched)) {
      return [];
    }
    return Object.keys(control.errors ?? {})
      .filter(clave => this.mensajes()[clave])
      .map(clave => this.mensajes()[clave]);
  }
}
