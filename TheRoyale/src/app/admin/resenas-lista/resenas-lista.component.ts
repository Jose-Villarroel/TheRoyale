import { Component, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CabeceraComponent } from '../../shared/cabecera/cabecera.component';
import { MenuMovilComponent } from '../../shared/menu-movil/menu-movil.component';
import { PiePaginaComponent } from '../../shared/pie-pagina/pie-pagina.component';
import { HeroInteriorComponent } from '../../shared/hero-interior/hero-interior.component';
import { AlertaErrorComponent } from '../../shared/alerta-error/alerta-error.component';
import { FiltrosEstadoComponent } from '../../shared/filtros-estado/filtros-estado.component';
import { NavAdminComponent } from '../secciones/nav-admin/nav-admin.component';
import { FiltrosResenasComponent } from './secciones/filtros-resenas/filtros-resenas.component';
import { GridResenasComponent } from './secciones/grid-resenas/grid-resenas.component';
import { ResenaService } from '../../services/resena.service';
import { TipoHabitacionService } from '../../services/tipo-habitacion.service';
import { EstadoVisibilidad, FILTROS_VISIBILIDAD_RESENA, Resena } from '../../models/resena.model';
import { TipoHabitacion } from '../../models/tipo-habitacion.model';

// Moderación de reseñas (/admin/resenas, solo admin): lista visibles y ocultas, filtra por
// tipo y calificación (en el API) y por visibilidad (en el cliente, con app-filtros-estado),
// y deja ocultar o volver a mostrar cada reseña.
@Component({
  selector: 'app-resenas-lista',
  standalone: true,
  imports: [
    RouterLink,
    CabeceraComponent,
    MenuMovilComponent,
    PiePaginaComponent,
    HeroInteriorComponent,
    AlertaErrorComponent,
    FiltrosEstadoComponent,
    NavAdminComponent,
    FiltrosResenasComponent,
    GridResenasComponent
  ],
  templateUrl: './resenas-lista.component.html'
})
export class ResenasListaComponent implements OnInit {
  private resenaService = inject(ResenaService);
  private tipoHabitacionService = inject(TipoHabitacionService);

  resenas: Resena[] = [];
  tipos: TipoHabitacion[] = [];
  tipoActivo: number | null = null;
  calificacionActiva: number | null = null;
  visibilidadActiva: EstadoVisibilidad | null = null;
  procesandoId: number | null = null;
  error: string | null = null;
  mensaje: string | null = null;
  menuAbierto = false;

  readonly filtrosVisibilidad = FILTROS_VISIBILIDAD_RESENA;

  ngOnInit(): void {
    this.tipoHabitacionService.obtenerTodos().subscribe({
      next: (tipos) => this.tipos = tipos,
      error: () => this.tipos = []
    });
    this.cargar();
  }

  // El API no filtra por visibilidad: se aplica sobre la lista ya cargada
  get resenasFiltradas(): Resena[] {
    switch (this.visibilidadActiva) {
      case 'VISIBLE':
        return this.resenas.filter(r => r.visible);
      case 'OCULTA':
        return this.resenas.filter(r => !r.visible);
      default:
        return this.resenas;
    }
  }

  filtrarTipo(tipoId: number | null): void {
    this.tipoActivo = tipoId;
    this.cargar();
  }

  filtrarCalificacion(calificacion: number | null): void {
    this.calificacionActiva = calificacion;
    this.cargar();
  }

  filtrarVisibilidad(visibilidad: EstadoVisibilidad | null): void {
    this.visibilidadActiva = visibilidad;
  }

  ocultar(resena: Resena): void {
    this.cambiarVisibilidad(resena, false);
  }

  mostrar(resena: Resena): void {
    this.cambiarVisibilidad(resena, true);
  }

  private cambiarVisibilidad(resena: Resena, visible: boolean): void {
    this.procesandoId = resena.id;
    this.error = null;
    this.mensaje = null;
    const peticion = visible ? this.resenaService.mostrar(resena.id) : this.resenaService.ocultar(resena.id);
    peticion.subscribe({
      next: (actualizada) => {
        this.procesandoId = null;
        this.resenas = this.resenas.map(r => r.id === actualizada.id ? actualizada : r);
        this.mensaje = visible
          ? `Review #${resena.id} is visible again.`
          : `Review #${resena.id} is now hidden from the public.`;
      },
      error: (err) => {
        this.procesandoId = null;
        this.error = err.error?.message || err.message || 'Error updating the review';
      }
    });
  }

  private cargar(): void {
    this.resenaService.obtenerParaModeracion({
      tipoHabitacionId: this.tipoActivo,
      calificacion: this.calificacionActiva
    }).subscribe({
      next: (resenas) => {
        this.resenas = resenas;
        this.error = null;
      },
      error: (err) => {
        this.error = err.error?.message || err.message || 'Error loading reviews';
      }
    });
  }
}
