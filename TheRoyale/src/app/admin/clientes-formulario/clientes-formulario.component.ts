import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ClienteService } from '../../services/cliente.service';
import { CabeceraComponent } from '../../shared/cabecera/cabecera.component';
import { MenuMovilComponent } from '../../shared/menu-movil/menu-movil.component';
import { PiePaginaComponent } from '../../shared/pie-pagina/pie-pagina.component';
import { HeroInteriorComponent } from '../../shared/hero-interior/hero-interior.component';
import { AlertaErrorComponent } from '../secciones/alerta-error/alerta-error.component';
import { CampoErrorComponent } from '../tipos-habitacion-formulario/secciones/campo-error/campo-error.component';

@Component({
  selector: 'app-clientes-formulario',
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
  templateUrl: './clientes-formulario.component.html'
})
export class ClientesFormularioComponent implements OnInit {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private clienteService = inject(ClienteService);

  // Si id es null el guardado crea; si tiene id, actualiza
  id: number | null = null;
  error: string | null = null;
  menuAbierto = false;

  // Mismos límites que las columnas de Cliente en el backend.
  // El password es obligatorio solo al crear: al editar se quita el validador (vacío = conservar).
  clienteForm = this.fb.nonNullable.group({
    nombre: ['', [Validators.required, Validators.maxLength(100)]],
    apellido: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
    telefono: ['', [Validators.maxLength(20)]],
    password: ['', [Validators.required]]
  });

  ngOnInit(): void {
    const parametroId = this.route.snapshot.paramMap.get('id');
    if (parametroId === null) {
      return;
    }

    const password = this.clienteForm.controls.password;
    password.clearValidators();
    password.updateValueAndValidity();

    this.clienteService.obtenerPorId(Number(parametroId)).subscribe({
      next: (cliente) => {
        this.id = cliente.id;
        this.clienteForm.patchValue({
          nombre: cliente.nombre,
          apellido: cliente.apellido,
          email: cliente.email,
          telefono: cliente.telefono ?? ''
        });
      },
      error: () => {
        this.router.navigate(['/admin/clientes']);
      }
    });
  }

  guardar(): void {
    if (this.clienteForm.invalid) {
      this.clienteForm.markAllAsTouched();
      return;
    }

    const datos = this.clienteForm.getRawValue();

    if (this.id === null) {
      this.clienteService.crear(datos).subscribe({
        next: () => this.router.navigate(['/admin/clientes']),
        error: (ex) => {
          this.error = ex.error?.message || ex.message || 'Error creating client';
        }
      });
    } else {
      this.clienteService.actualizar(this.id, datos).subscribe({
        next: () => this.router.navigate(['/admin/clientes']),
        error: (ex) => {
          this.error = ex.error?.message || ex.message || 'Error updating client';
        }
      });
    }
  }
}
