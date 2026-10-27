import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

// Componente raíz: solo aloja el <router-outlet>; cada página se carga según app.routes.ts.
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  template: '<router-outlet></router-outlet>'
})
export class AppComponent {}