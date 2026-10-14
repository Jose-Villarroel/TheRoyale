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

  constructor(private authService: AuthService, private router: Router, private route: ActivatedRoute) {}

  // Mismos avisos que mandaba el controlador Thymeleaf por query params (?mensaje=accountCreated...)
  ngOnInit(): void {
    const parametros = this.route.snapshot.queryParamMap;
    this.email = parametros.get('email') ?? '';
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
      // El cliente aterriza en su perfil; el operador no tiene perfil de cliente y vuelve al inicio
      next: (usuario) => this.router.navigate([usuario.tipo === 'CLIENTE' ? '/profile' : '/']),
      error: () => {
        this.error = 'Invalid credentials. Please try again.';
        this.cargando = false;
      }
    });
  }
}
