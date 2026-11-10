import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin, map, switchMap } from 'rxjs';
import { CabeceraComponent } from '../../shared/cabecera/cabecera.component';
import { MenuMovilComponent } from '../../shared/menu-movil/menu-movil.component';
import { PiePaginaComponent } from '../../shared/pie-pagina/pie-pagina.component';
import { ImagenDetalleComponent } from '../../shared/imagen-detalle/imagen-detalle.component';
import { InfoRoomComponent } from './secciones/info-room/info-room.component';
import { DisponibilidadRoomComponent } from './secciones/disponibilidad-room/disponibilidad-room.component';
import { Visor3dComponent } from './secciones/visor-3d/visor-3d.component';
import { CtaInteriorComponent } from '../../shared/cta-interior/cta-interior.component';
import { TipoHabitacionService } from '../../services/tipo-habitacion.service';
import { HabitacionService } from '../../services/habitacion.service';
import { TipoHabitacion } from '../../models/tipo-habitacion.model';
import { Habitacion } from '../../models/habitacion.model';

@Component({
  selector: 'app-room-detalle',
  standalone: true,
  imports: [
    CabeceraComponent,
    MenuMovilComponent,
    PiePaginaComponent,
    ImagenDetalleComponent,
    InfoRoomComponent,
    DisponibilidadRoomComponent,
    Visor3dComponent,
    CtaInteriorComponent
  ],
  templateUrl: './room-detalle.component.html'
})
export class RoomDetalleComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private tipoHabitacionService = inject(TipoHabitacionService);
  private habitacionService = inject(HabitacionService);

  tipo: TipoHabitacion | null = null;
  habitaciones: Habitacion[] = [];
  precioDesde: number | null = null;
  habitacionesDisponibles = 0;
  menuAbierto = false;

  ngOnInit(): void {
    this.route.paramMap.pipe(
      map(params => Number(params.get('id'))),
      switchMap(id => {
        if (!id || Number.isNaN(id)) {
          this.router.navigate(['/rooms']);
          return [];
        }
        return forkJoin({
          tipo: this.tipoHabitacionService.obtenerPorId(id),
          habitaciones: this.habitacionService.obtenerPorTipoHabitacionId(id)
        });
      })
    ).subscribe({
      next: ({ tipo, habitaciones }: { tipo: TipoHabitacion; habitaciones: Habitacion[] }) => {
        this.tipo = tipo;
        this.habitaciones = habitaciones;
        this.habitacionesDisponibles = habitaciones.filter((h: Habitacion) => h.estado === 'DISPONIBLE').length;

        const precios = habitaciones
          .map((h: Habitacion) => h.precio)
          .filter((p: number | null | undefined): p is number => p !== null && p !== undefined);
        this.precioDesde = precios.length > 0 ? Math.min(...precios) : null;
      },
      error: () => this.router.navigate(['/rooms'])
    });
  }
}
