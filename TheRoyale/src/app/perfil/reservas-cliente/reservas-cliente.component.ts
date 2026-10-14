import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CabeceraComponent } from '../../shared/cabecera/cabecera.component';
import { MenuMovilComponent } from '../../shared/menu-movil/menu-movil.component';
import { PiePaginaComponent } from '../../shared/pie-pagina/pie-pagina.component';
import { AuthService } from '../../services/auth.service';

// Equivale a reservations.html: bienvenida del área de huésped con accesos al perfil y a cerrar sesión
@Component({
  selector: 'app-reservas-cliente',
  standalone: true,
  imports: [RouterLink, CabeceraComponent, MenuMovilComponent, PiePaginaComponent],
  templateUrl: './reservas-cliente.component.html'
})
export class ReservasClienteComponent {
  private router = inject(Router);
  authService = inject(AuthService);

  menuAbierto = false;

  cerrarSesion(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
