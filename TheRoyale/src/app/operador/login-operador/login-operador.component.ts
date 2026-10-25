import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

// Puerta de entrada del personal del hotel, separada del login de huéspedes.
// Usa el mismo POST /api/auth/login (el backend ya distingue cliente de operador por el
// campo "tipo"), pero rechaza a quien no sea operador en vez de dejarlo pasar al portal.
@Component({
  selector: 'app-login-operador',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './login-operador.component.html'
})
export class LoginOperadorComponent implements OnInit {
  email = '';
  password = '';
  error = '';
  cargando = false;

  private redirect: string | null = null;

  constructor(private authService: AuthService, private router: Router, private route: ActivatedRoute) {}

  ngOnInit(): void {
    this.redirect = this.route.snapshot.queryParamMap.get('redirect');
  }

  iniciarSesion(): void {
    this.error = '';
    this.cargando = true;
    this.authService.login({ email: this.email, password: this.password }).subscribe({
      next: (usuario) => {
        if (usuario.tipo !== 'OPERADOR') {
          // Credenciales válidas pero de un huésped: no se le abre el portal de gestión
          this.authService.logout();
          this.error = 'This login is for hotel staff only. Guests should sign in from the main page.';
          this.cargando = false;
          return;
        }
        this.router.navigateByUrl(this.redirect ?? '/operator/reservas');
      },
      error: () => {
        this.error = 'Invalid credentials. Please try again.';
        this.cargando = false;
      }
    });
  }
}
