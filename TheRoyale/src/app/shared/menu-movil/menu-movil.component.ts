import { Component, EventEmitter, inject, Input, Output } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-menu-movil',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './menu-movil.component.html'
})
export class MenuMovilComponent {
  private router = inject(Router);
  authService = inject(AuthService);

  @Input() abierto = false;
  @Output() cerrar = new EventEmitter<void>();

  cerrarSesion(): void {
    this.authService.logout();
    this.cerrar.emit();
    this.router.navigate(['/login']);
  }
}
