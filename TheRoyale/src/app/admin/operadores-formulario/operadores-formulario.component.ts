import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { OperadorService } from '../../services/operador.service';
import { CabeceraComponent } from '../../shared/cabecera/cabecera.component';
import { MenuMovilComponent } from '../../shared/menu-movil/menu-movil.component';
import { PiePaginaComponent } from '../../shared/pie-pagina/pie-pagina.component';
import { HeroInteriorComponent } from '../../shared/hero-interior/hero-interior.component';
import { AlertaErrorComponent } from '../../shared/alerta-error/alerta-error.component';
import { CampoErrorComponent } from '../../shared/campo-error/campo-error.component';

@Component({
  selector: 'app-operadores-formulario',
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
  templateUrl: './operadores-formulario.component.html'
})
export class OperadoresFormularioComponent implements OnInit {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private operadorService = inject(OperadorService);

  // Si id es null el guardado crea; si tiene id, actualiza
  id: number | null = null;
  error: string | null = null;
  menuAbierto = false;

  // Mismos límites que las columnas de Operador en el backend.
  // El password es obligatorio solo al crear: al editar se quita el validador (vacío = conservar).
  operadorForm = this.fb.nonNullable.group({
    nombre: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
    password: ['', [Validators.required]]
  });

  ngOnInit(): void {
    const parametroId = this.route.snapshot.paramMap.get('id');
    if (parametroId === null) {
      return;
    }

    const password = this.operadorForm.controls.password;
    password.clearValidators();
    password.updateValueAndValidity();

    this.operadorService.obtenerPorId(Number(parametroId)).subscribe({
      next: (operador) => {
        this.id = operador.id;
        this.operadorForm.patchValue({
          nombre: operador.nombre,
          email: operador.email
        });
      },
      error: () => {
        this.router.navigate(['/admin/operadores']);
      }
    });
  }

  guardar(): void {
    if (this.operadorForm.invalid) {
      this.operadorForm.markAllAsTouched();
      return;
    }

    // TipoOperador solo tiene un valor, así que no se pide en el formulario
    const datos = { ...this.operadorForm.getRawValue(), tipo: 'OPERADOR' as const };

    if (this.id === null) {
      this.operadorService.crear(datos).subscribe({
        next: () => this.router.navigate(['/admin/operadores']),
        error: (ex) => {
          this.error = ex.error?.message || ex.message || 'Error creating operator';
        }
      });
    } else {
      this.operadorService.actualizar(this.id, datos).subscribe({
        next: () => this.router.navigate(['/admin/operadores']),
        error: (ex) => {
          this.error = ex.error?.message || ex.message || 'Error updating operator';
        }
      });
    }
  }
}
