import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

// Navegación entre las secciones del panel admin.
// routerLinkActive marca la página actual, así que no necesita estado ni inputs.
@Component({
  selector: 'app-nav-admin',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './nav-admin.component.html'
})
export class NavAdminComponent {}
