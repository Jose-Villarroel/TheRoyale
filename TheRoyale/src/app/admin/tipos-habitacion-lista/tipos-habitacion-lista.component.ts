import { Component, inject, OnInit } from '@angular/core';
import { CabeceraComponent } from '../../shared/cabecera/cabecera.component';
import { MenuMovilComponent } from '../../shared/menu-movil/menu-movil.component';
import { PiePaginaComponent } from '../../shared/pie-pagina/pie-pagina.component';
import { HeroInteriorComponent } from '../../shared/hero-interior/hero-interior.component';
import { AlertaErrorComponent } from '../secciones/alerta-error/alerta-error.component';
import { NavAdminComponent } from '../secciones/nav-admin/nav-admin.component';
import { EncabezadoListaComponent } from './secciones/encabezado-lista/encabezado-lista.component';
import { GridTiposHabitacionComponent } from './secciones/grid-tipos-habitacion/grid-tipos-habitacion.component';
import { TipoHabitacionService } from '../../services/tipo-habitacion.service';
import { TipoHabitacion } from '../../models/tipo-habitacion.model';

// Página contenedora: trae los datos del servicio y maneja el borrado.
// La parte visual vive en los componentes de secciones/.
@Component({
  selector: 'app-tipos-habitacion-lista',
  standalone: true,
  imports: [
    CabeceraComponent,
    MenuMovilComponent,
    PiePaginaComponent,
    HeroInteriorComponent,
    AlertaErrorComponent,
    NavAdminComponent,
    EncabezadoListaComponent,
    GridTiposHabitacionComponent
  ],
  templateUrl: './tipos-habitacion-lista.component.html'
})
export class TiposHabitacionListaComponent implements OnInit {
  private tipoHabitacionService = inject(TipoHabitacionService);

  tipos: TipoHabitacion[] = [];
  error: string | null = null;
  menuAbierto = false;

  ngOnInit(): void {
    this.cargarTipos();
  }

  eliminar(tipo: TipoHabitacion): void {
    if (!confirm('Delete this room type?')) {
      return;
    }
    this.tipoHabitacionService.eliminar(tipo.id).subscribe({
      next: () => {
        this.error = null;
        this.cargarTipos();
      },
      error: (err) => {
        this.error = err.error?.message || err.message || 'Error deleting room type';
      }
    });
  }

  private cargarTipos(): void {
    this.tipoHabitacionService.obtenerTodos().subscribe({
      next: (tipos) => {
        this.tipos = tipos;
      },
      error: (err) => {
        this.error = err.error?.message || err.message || 'Error loading room types';
      }
    });
  }
}
