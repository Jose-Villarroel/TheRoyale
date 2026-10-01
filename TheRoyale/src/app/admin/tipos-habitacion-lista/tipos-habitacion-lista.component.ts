import { Component, inject, OnInit } from '@angular/core';
import { CabeceraComponent } from '../../shared/cabecera/cabecera.component';
import { MenuMovilComponent } from '../../shared/menu-movil/menu-movil.component';
import { PiePaginaComponent } from '../../shared/pie-pagina/pie-pagina.component';
import { HeroAdminComponent } from '../secciones/hero-admin/hero-admin.component';
import { AlertaErrorComponent } from '../secciones/alerta-error/alerta-error.component';
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
    HeroAdminComponent,
    AlertaErrorComponent,
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
    try {
      this.tipoHabitacionService.eliminar(tipo.id);
      this.error = null;
      this.cargarTipos();
    } catch (ex) {
      this.error = (ex as Error).message;
    }
  }

  // El servicio devuelve siempre el mismo array (y lo muta); se copia para que el
  // input() de la grilla reciba una referencia nueva y se vuelva a pintar.
  private cargarTipos(): void {
    this.tipos = [...this.tipoHabitacionService.obtenerTodos()];
  }
}
