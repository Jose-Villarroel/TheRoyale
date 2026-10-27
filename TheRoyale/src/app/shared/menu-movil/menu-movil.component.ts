import { Component, EventEmitter, inject, Input, Output } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

// Menú lateral para pantallas pequeñas; se abre desde la cabecera y avisa (cerrar) al salir.
// Muestra enlaces distintos según haya sesión de huésped, operador o admin.
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
  // Igual que en la cabecera: en la landing no aparece el acceso Admin
  @Input() mostrarAccesosStaff = true;
  @Output() cerrar = new EventEmitter<void>();

  cerrarSesion(): void {
    this.authService.logout();
    this.cerrar.emit();
    this.router.navigate(['/login']);
  }
}
