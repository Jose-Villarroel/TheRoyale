import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TipoHabitacionService } from '../../services/tipo-habitacion.service';
import { TipoHabitacion } from '../../models/tipo-habitacion.model';

@Component({
  selector: 'app-tipos-habitacion-formulario',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './tipos-habitacion-formulario.component.html'
})
export class TiposHabitacionFormularioComponent implements OnInit {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private tipoHabitacionService = inject(TipoHabitacionService);

  // Si id es null el guardado crea; si tiene id, actualiza (igual que el campo oculto de Thymeleaf)
  id: number | null = null;
  error: string | null = null;

  // Mismos límites que las columnas de TipoHabitacion en el backend
  tipoForm: FormGroup = this.fb.group({
    nombre: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(100)]],
    descripcion: ['', [Validators.required, Validators.maxLength(500)]],
    imagenUrl: ['', [Validators.maxLength(255)]]
  });

  ngOnInit(): void {
    const parametroId = this.route.snapshot.paramMap.get('id');
    if (parametroId === null) {
      return;
    }

    const tipo = this.tipoHabitacionService.obtenerPorId(Number(parametroId));
    if (!tipo) {
      this.router.navigate(['/admin/tipos-habitacion']);
      return;
    }
    this.id = tipo.id;
    this.tipoForm.patchValue({
      nombre: tipo.nombre,
      descripcion: tipo.descripcion,
      imagenUrl: tipo.imagenUrl
    });
  }

  guardar(): void {
    if (this.tipoForm.invalid) {
      this.tipoForm.markAllAsTouched();
      return;
    }

    const valores = this.tipoForm.value;
    const datos = {
      nombre: valores.nombre,
      descripcion: valores.descripcion,
      imagenUrl: valores.imagenUrl || ''
    };

    // El servicio sigue validando reglas de negocio (nombre único), por eso se mantiene el try/catch
    try {
      if (this.id === null) {
        this.tipoHabitacionService.crear(datos);
      } else {
        const tipo: TipoHabitacion = { id: this.id, ...datos };
        this.tipoHabitacionService.actualizar(tipo);
      }
      this.router.navigate(['/admin/tipos-habitacion']);
    } catch (ex) {
      this.error = (ex as Error).message;
    }
  }
}
