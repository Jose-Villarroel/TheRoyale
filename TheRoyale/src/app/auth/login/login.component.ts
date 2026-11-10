import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, CommonModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {
  email = '';
  password = '';
  error = '';
  mensaje = '';
  cargando = false;

  private redirect: string | null = null;

  constructor(private authService: AuthService, private router: Router, private route: ActivatedRoute) {}

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
        if (this.redirect) {
          this.router.navigateByUrl(this.redirect);
          return;
        }
        const destinos: Record<string, string> = {
          CLIENTE: '/profile',
          OPERADOR: '/operator/reservas',
          ADMIN: '/admin/dashboard'
        };
        this.router.navigate([destinos[usuario.tipo] ?? '/']);
      },
      error: () => {
        this.error = 'Invalid credentials. Please try again.';
        this.cargando = false;
      }
    });
  }

  iniciarSesionConGoogle(): void {
    this.cargando = true;
    this.error = '';
    const emailPrompt = prompt('Enter your Google email to sign in:', 'guest.google@theroyale.com');
    if (!emailPrompt) {
      this.cargando = false;
      return;
    }
    const nombre = emailPrompt.split('@')[0];
    this.authService.loginConGoogle(emailPrompt, nombre, 'GoogleUser').subscribe({
      next: () => {
        if (this.redirect) {
          this.router.navigateByUrl(this.redirect);
          return;
        }
        this.router.navigate(['/profile']);
      },
      error: () => {
        this.error = 'Google Sign-In failed. Please try again.';
        this.cargando = false;
      }
    });
  }
}
