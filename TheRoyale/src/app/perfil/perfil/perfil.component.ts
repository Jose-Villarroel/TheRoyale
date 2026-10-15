import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CabeceraComponent } from '../../shared/cabecera/cabecera.component';
import { MenuMovilComponent } from '../../shared/menu-movil/menu-movil.component';
import { PiePaginaComponent } from '../../shared/pie-pagina/pie-pagina.component';
import { HeroInteriorComponent } from '../../shared/hero-interior/hero-interior.component';
import { AlertaErrorComponent } from '../../admin/secciones/alerta-error/alerta-error.component';
import { CampoErrorComponent } from '../../admin/tipos-habitacion-formulario/secciones/campo-error/campo-error.component';
import { AuthService } from '../../services/auth.service';
import { ClienteService } from '../../services/cliente.service';
import { Cliente } from '../../models/cliente.model';

// Equivale a profile.html: el cliente ve sus datos, los edita o borra su cuenta.
// El id sale de la sesión (AuthService), no de la URL como en Thymeleaf (?clienteId=).
@Component({
  selector: 'app-perfil',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    CabeceraComponent,
    MenuMovilComponent,
    PiePaginaComponent,
    HeroInteriorComponent,
    AlertaErrorComponent,
    CampoErrorComponent
  ],
  templateUrl: './perfil.component.html'
})
export class PerfilComponent implements OnInit {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private authService = inject(AuthService);
  private clienteService = inject(ClienteService);

  cliente: Cliente | null = null;
  mensaje: string | null = null;
  error: string | null = null;
  menuAbierto = false;

  // Password vacío = conservar el actual (ClienteService.actualizar en el backend lo ignora)
  perfilForm = this.fb.nonNullable.group({
    nombre: ['', [Validators.required, Validators.maxLength(100)]],
    apellido: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
    telefono: ['', [Validators.maxLength(20)]],
    password: ['']
  });

  ngOnInit(): void {
    const usuario = this.authService.usuario;
    if (!usuario || !this.authService.esCliente) {
      this.router.navigate(['/login']);
      return;
    }

    this.clienteService.obtenerPorId(usuario.id).subscribe({
      next: (cliente) => this.mostrarCliente(cliente),
      error: () => {
        // La cuenta ya no existe en la base: se cierra la sesión
        this.authService.logout();
        this.router.navigate(['/login']);
      }
    });
  }

  guardar(): void {
    if (!this.cliente || this.perfilForm.invalid) {
      this.perfilForm.markAllAsTouched();
      return;
    }

    this.mensaje = null;
    this.error = null;
    this.clienteService.actualizar(this.cliente.id, this.perfilForm.getRawValue()).subscribe({
      next: (cliente) => {
        this.mostrarCliente(cliente);
        this.authService.actualizarUsuario(cliente);
        this.mensaje = 'Profile updated successfully.';
      },
      error: (ex) => {
        this.error = ex.error?.message || ex.message || 'Error updating profile';
      }
    });
  }

  cancelar(): void {
    if (this.cliente) {
      this.mostrarCliente(this.cliente);
    }
  }

  eliminar(): void {
    if (!this.cliente || !confirm('Delete your client profile? This action cannot be undone.')) {
      return;
    }

    this.clienteService.eliminar(this.cliente.id).subscribe({
      next: () => {
        this.authService.logout();
        this.router.navigate(['/login'], { queryParams: { deleted: 1 } });
      },
      error: (ex) => {
        // 409 si el cliente tiene reservas asociadas
        this.mensaje = null;
        this.error = ex.error?.message || ex.message || 'Error deleting profile';
      }
    });
  }

  cerrarSesion(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  private mostrarCliente(cliente: Cliente): void {
    this.cliente = cliente;
    this.perfilForm.reset({
      nombre: cliente.nombre,
      apellido: cliente.apellido,
      email: cliente.email,
      telefono: cliente.telefono ?? '',
      password: ''
    });
  }
}
