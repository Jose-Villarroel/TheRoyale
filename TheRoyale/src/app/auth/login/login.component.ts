import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, CommonModule, RouterLink],
  templateUrl: './login.component.html'
})
export class LoginComponent implements OnInit {
  email = '';
  password = '';
  error = '';
  mensaje = '';
  cargando = false;

  // Ruta a la que volver tras iniciar sesion (la pone clienteGuard o el boton de reservar)
  private redirect: string | null = null;

  constructor(private authService: AuthService, private router: Router, private route: ActivatedRoute) {}

  // Mismos avisos que mandaba el controlador Thymeleaf por query params (?mensaje=accountCreated...)
  ngOnInit(): void {
    const parametros = this.route.snapshot.queryParamMap;
    this.email = parametros.get('email') ?? '';
    this.redirect = parametros.get('redirect');
    if (parametros.has('registered')) {
      this.mensaje = 'Account created. You can sign in now.';
    } else if (parametros.has('deleted')) {
      this.mensaje = 'Your profile was deleted.';
    }
  }

  iniciarSesion(): void {
    this.error = '';
    this.mensaje = '';
    this.cargando = true;
    this.authService.login({ email: this.email, password: this.password }).subscribe({
      next: (usuario) => {
        // Si venia de una pagina protegida vuelve alli; si no, cada rol entra a su propio panel:
        // el cliente a su perfil, el operador a operaciones y el admin a gestión.
        if (this.redirect) {
          this.router.navigateByUrl(this.redirect);
          return;
        }
        const destinos: Record<string, string> = {
          CLIENTE: '/profile',
          OPERADOR: '/operator/reservas',
          ADMIN: '/admin/reservas'
        };
        this.router.navigate([destinos[usuario.tipo] ?? '/']);
      },
      error: () => {
        this.error = 'Invalid credentials. Please try again.';
        this.cargando = false;
      }
    });
  }
}
