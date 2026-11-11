import { Component, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Cliente } from '../../../../models/cliente.model';

// Panel lateral del perfil ("Client Record"): datos del cliente, enlace a sus reservas
// y cerrar sesión, como el <aside> de profile.html.
// Selector de atributo: el <aside> debe ser hijo directo de .perfil-grid.
@Component({
  selector: 'aside[app-resumen-perfil]',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './resumen-perfil.component.html',
  host: { class: 'perfil-resumen' }
})
export class ResumenPerfilComponent {
  cliente = input.required<Cliente>();
  cerrarSesion = output<void>();
}
