import { Component, input } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { DisponibilidadTipo } from '../../../../models/reserva.model';

// Paso 03 de la reserva: resumen de lo elegido, total estimado y el botón de confirmar.
// El botón es type="submit": como el componente vive dentro del <form> de la página,
// el envío lo recibe el (ngSubmit) de la página. Sin sesión de huésped muestra "Sign In".
// Selector de atributo: el <section class="reserva-paso"> queda hijo directo del <form>.
@Component({
  selector: 'section[app-resumen-reserva]',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, RouterLink],
  templateUrl: './resumen-reserva.component.html',
  host: { class: 'reserva-paso reserva-paso-ultimo' }
})
export class ResumenReservaComponent {
  fechaInicio = input<string | null>(null);
  fechaFin = input<string | null>(null);
  noches = input(0);
  cantidadPersonas = input<number | null>(null);
  seleccionado = input<DisponibilidadTipo | null>(null);
  totalEstimado = input(0);

  esCliente = input(false);
  esOperador = input(false);
  puedeConfirmar = input(false);
  enviando = input(false);
  enEdicion = input(false);
  // Vuelve al formulario con lo ya elegido después de iniciar sesión
  urlRetorno = input('');
}
