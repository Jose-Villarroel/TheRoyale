import { Component, inject, OnInit } from '@angular/core';
import { CabeceraComponent } from '../../shared/cabecera/cabecera.component';
import { MenuMovilComponent } from '../../shared/menu-movil/menu-movil.component';
import { PiePaginaComponent } from '../../shared/pie-pagina/pie-pagina.component';
import { HeroInteriorComponent } from '../../shared/hero-interior/hero-interior.component';
import { AlertaErrorComponent } from '../secciones/alerta-error/alerta-error.component';
import { NavAdminComponent } from '../secciones/nav-admin/nav-admin.component';
import { EncabezadoHabitacionesComponent } from './secciones/encabezado-habitaciones/encabezado-habitaciones.component';
import { GridHabitacionesComponent } from './secciones/grid-habitaciones/grid-habitaciones.component';
import { HabitacionService } from '../../services/habitacion.service';
import { Habitacion } from '../../models/habitacion.model';

// Página contenedora: trae los datos del servicio y maneja el borrado.
// La parte visual vive en los componentes de secciones/.
@Component({
  selector: 'app-habitaciones-lista',
  standalone: true,
  imports: [
    CabeceraComponent,
    MenuMovilComponent,
    PiePaginaComponent,
    HeroInteriorComponent,
    AlertaErrorComponent,
    NavAdminComponent,
    EncabezadoHabitacionesComponent,
    GridHabitacionesComponent
  ],
  templateUrl: './habitaciones-lista.component.html'
})
export class HabitacionesListaComponent implements OnInit {
  private habitacionService = inject(HabitacionService);

  habitaciones: Habitacion[] = [];
  error: string | null = null;
  menuAbierto = false;

  ngOnInit(): void {
    this.cargarHabitaciones();
  }

  eliminar(habitacion: Habitacion): void {
    if (!confirm('Delete this room?')) {
      return;
    }
    this.habitacionService.eliminar(habitacion.id).subscribe({
      next: () => {
        this.error = null;
        this.cargarHabitaciones();
      },
      error: (err) => {
        // 409 si la habitación tiene reservas asociadas
        this.error = err.error?.message || err.message || 'Error deleting room';
      }
    });
  }

  private cargarHabitaciones(): void {
    this.habitacionService.obtenerTodos().subscribe({
      next: (habitaciones) => {
        this.habitaciones = habitaciones;
      },
      error: (err) => {
        this.error = err.error?.message || err.message || 'Error loading rooms';
      }
    });
  }
}
