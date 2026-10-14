import { Component, EventEmitter, HostListener, inject, Input, Output } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-cabecera',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './cabecera.component.html'
})
export class CabeceraComponent {
  private router = inject(Router);
  // Público para que la plantilla muestre Sign In / My Profile según haya sesión
  authService = inject(AuthService);

  // El menú móvil es hermano de la cabecera, así que quien la usa decide cuándo abrirlo.
  @Output() abrirMenu = new EventEmitter<void>();

  // Páginas interiores sin hero oscuro detrás: cabecera sólida desde el inicio (como layout.html)
  @Input() siempreSolida = false;

  // Cabecera sólida una vez que se bajan 90px, igual que antes pero con binding en vez de classList
  solida = false;

  @HostListener('window:scroll')
  alHacerScroll(): void {
    this.solida = (window.scrollY || 0) > 90;
  }

  cerrarSesion(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
