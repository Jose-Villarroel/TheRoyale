import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HabitacionService } from '../../services/habitacion.service';
import { TipoHabitacionService } from '../../services/tipo-habitacion.service';
import { EstadoHabitacion, HabitacionRequest } from '../../models/habitacion.model';
import { TipoHabitacion } from '../../models/tipo-habitacion.model';
import { CabeceraComponent } from '../../shared/cabecera/cabecera.component';
import { MenuMovilComponent } from '../../shared/menu-movil/menu-movil.component';
import { PiePaginaComponent } from '../../shared/pie-pagina/pie-pagina.component';
import { HeroInteriorComponent } from '../../shared/hero-interior/hero-interior.component';
import { AlertaErrorComponent } from '../../shared/alerta-error/alerta-error.component';
import { CampoErrorComponent } from '../../shared/campo-error/campo-error.component';

@Component({
  selector: 'app-habitaciones-formulario',
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
  templateUrl: './habitaciones-formulario.component.html'
})
export class HabitacionesFormularioComponent implements OnInit {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private habitacionService = inject(HabitacionService);
  private tipoHabitacionService = inject(TipoHabitacionService);

  // Mismos valores que el enum EstadoHabitacion del backend
  readonly estados: EstadoHabitacion[] = ['DISPONIBLE', 'OCUPADA', 'MANTENIMIENTO', 'DESHABILITADA'];

  // Si id es null el guardado crea; si tiene id, actualiza
  id: number | null = null;
  tipos: TipoHabitacion[] = [];
  error: string | null = null;
  menuAbierto = false;

  // Mismos límites que las columnas de Habitacion en el backend
  habitacionForm: FormGroup = this.fb.group({
    numero: ['', [Validators.required, Validators.maxLength(10)]],
    tipoHabitacionId: [null as number | null, [Validators.required]],
    precio: [null as number | null, [Validators.required, Validators.min(0)]],
    estado: ['DISPONIBLE' as EstadoHabitacion, [Validators.required]]
  });

  ngOnInit(): void {
    this.tipoHabitacionService.obtenerTodos().subscribe({
      next: (tipos) => this.tipos = tipos,
      error: (err) => this.error = err.error?.message || err.message || 'Error loading room types'
    });

    const parametroId = this.route.snapshot.paramMap.get('id');
    if (parametroId === null) {
      return;
    }

    this.habitacionService.obtenerPorId(Number(parametroId)).subscribe({
      next: (habitacion) => {
        this.id = habitacion.id;
        this.habitacionForm.patchValue({
          numero: habitacion.numero,
          tipoHabitacionId: habitacion.tipoHabitacionId,
          precio: habitacion.precio,
          estado: habitacion.estado
        });
      },
      error: () => {
        this.router.navigate(['/admin/habitaciones']);
      }
    });
  }

  guardar(): void {
    if (this.habitacionForm.invalid) {
      this.habitacionForm.markAllAsTouched();
      return;
    }

    const valores = this.habitacionForm.value;
    const datos: HabitacionRequest = {
      numero: valores.numero.trim(),
      tipoHabitacionId: Number(valores.tipoHabitacionId),
      precio: Number(valores.precio),
      estado: valores.estado
    };

    if (this.id === null) {
      this.habitacionService.crear(datos).subscribe({
        next: () => this.router.navigate(['/admin/habitaciones']),
        error: (ex) => {
          this.error = ex.error?.message || ex.message || 'Error creating room';
        }
      });
    } else {
      this.habitacionService.actualizar(this.id, datos).subscribe({
        next: () => this.router.navigate(['/admin/habitaciones']),
        error: (ex) => {
          this.error = ex.error?.message || ex.message || 'Error updating room';
        }
      });
    }
  }
}
