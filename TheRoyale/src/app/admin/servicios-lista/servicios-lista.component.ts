import { Component, inject, OnInit } from '@angular/core';
import { CabeceraComponent } from '../../shared/cabecera/cabecera.component';
import { MenuMovilComponent } from '../../shared/menu-movil/menu-movil.component';
import { PiePaginaComponent } from '../../shared/pie-pagina/pie-pagina.component';
import { HeroInteriorComponent } from '../../shared/hero-interior/hero-interior.component';
import { AlertaErrorComponent } from '../secciones/alerta-error/alerta-error.component';
import { NavAdminComponent } from '../secciones/nav-admin/nav-admin.component';
import { EncabezadoServiciosComponent } from './secciones/encabezado-servicios/encabezado-servicios.component';
import { GridServiciosComponent } from './secciones/grid-servicios/grid-servicios.component';
import { ServicioService } from '../../services/servicio.service';
import { Servicio } from '../../models/servicio.model';

// Página contenedora: trae los datos del servicio y maneja el borrado.
// La parte visual vive en los componentes de secciones/.
@Component({
  selector: 'app-servicios-lista',
  standalone: true,
  imports: [
    CabeceraComponent,
    MenuMovilComponent,
    PiePaginaComponent,
    HeroInteriorComponent,
    AlertaErrorComponent,
    NavAdminComponent,
    EncabezadoServiciosComponent,
    GridServiciosComponent
  ],
  templateUrl: './servicios-lista.component.html'
})
export class ServiciosListaComponent implements OnInit {
  private servicioService = inject(ServicioService);

  servicios: Servicio[] = [];
  error: string | null = null;
  menuAbierto = false;

  ngOnInit(): void {
    this.cargarServicios();
  }

  eliminar(servicio: Servicio): void {
    if (!confirm('Delete this service?')) {
      return;
    }
    try {
      this.servicioService.eliminar(servicio.id);
      this.error = null;
      this.cargarServicios();
    } catch (ex) {
      this.error = (ex as Error).message;
    }
  }

  // El servicio devuelve siempre el mismo array (y lo muta); se copia para que el
  // input() de la grilla reciba una referencia nueva y se vuelva a pintar.
  private cargarServicios(): void {
    this.servicios = [...this.servicioService.obtenerTodos()];
  }
}
