import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-nav-operador',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './nav-operador.component.html'
})
export class NavOperadorComponent {
  private router = inject(Router);
  authService = inject(AuthService);

  cerrarSesion(): void {
    this.authService.logout();
    this.router.navigate(['/operator/login']);
  }
}
