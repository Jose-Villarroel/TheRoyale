import { Component, inject } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ClienteService } from '../../services/cliente.service';

// Validador de grupo: equivale al PASSWORD_NO_COINCIDE de AutenticacionService.registrarCliente
function passwordsIguales(grupo: AbstractControl): ValidationErrors | null {
  const password = grupo.get('password')?.value;
  const confirmacion = grupo.get('confirmPassword')?.value;
  return password && confirmacion && password !== confirmacion ? { passwordNoCoincide: true } : null;
}

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html'
})
export class RegisterComponent {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private clienteService = inject(ClienteService);

  error: string | null = null;
  cargando = false;

  // Mismos límites que las columnas de Cliente en el backend
  registroForm = this.fb.nonNullable.group({
    nombre: ['', [Validators.required, Validators.maxLength(100)]],
    apellido: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
    telefono: ['', [Validators.maxLength(20)]],
    password: ['', [Validators.required]],
    confirmPassword: ['', [Validators.required]]
  }, { validators: passwordsIguales });

  registrar(): void {
    if (this.registroForm.invalid) {
      this.registroForm.markAllAsTouched();
      return;
    }

    // confirmPassword solo sirve para validar en el front; el backend no lo espera
    const { confirmPassword, ...cliente } = this.registroForm.getRawValue();
    this.error = null;
    this.cargando = true;
    this.clienteService.crear(cliente).subscribe({
      next: (creado) => this.router.navigate(['/login'], { queryParams: { registered: 1, email: creado.email } }),
      error: (ex) => {
        // El backend responde 400 con el mensaje si el email ya existe
        this.error = ex.error?.message || 'Could not create the account. Please try again.';
        this.cargando = false;
      }
    });
  }

  mostrarError(campo: string): boolean {
    const control = this.registroForm.get(campo);
    return !!control && control.invalid && control.touched;
  }
}
