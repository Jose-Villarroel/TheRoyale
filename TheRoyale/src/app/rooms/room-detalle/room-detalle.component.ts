import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin, map, switchMap } from 'rxjs';
import { CabeceraComponent } from '../../shared/cabecera/cabecera.component';
import { MenuMovilComponent } from '../../shared/menu-movil/menu-movil.component';
import { PiePaginaComponent } from '../../shared/pie-pagina/pie-pagina.component';
import { ImagenDetalleComponent } from '../../shared/imagen-detalle/imagen-detalle.component';
import { InfoRoomComponent } from './secciones/info-room/info-room.component';
import { DisponibilidadRoomComponent } from './secciones/disponibilidad-room/disponibilidad-room.component';
import { CtaInteriorComponent } from '../../shared/cta-interior/cta-interior.component';
import { TipoHabitacionService } from '../../services/tipo-habitacion.service';
import { HabitacionService } from '../../services/habitacion.service';
import { TipoHabitacion } from '../../models/tipo-habitacion.model';
import { Habitacion } from '../../models/habitacion.model';

// Página contenedora del detalle de un tipo de habitación (/rooms/:id).
// Hace los mismos cálculos que RoomController.mostrarDetalleRoom y reparte los datos a las secciones.
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
  // null equivale al Optional vacío del backend: el tipo no tiene habitaciones con precio
  precioDesde: number | null = null;
  habitacionesDisponibles = 0;
  menuAbierto = false;

  // Se encadena desde paramMap (no desde snapshot) para que la pagina reaccione si cambia el :id
  // sin remontar el componente, y las dos peticiones se combinan con forkJoin en lugar de
  // anidar subscribes: ambas dependen del mismo id y sus resultados se necesitan juntos.
  ngOnInit(): void {
    this.route.paramMap.pipe(
      map(params => Number(params.get('id'))),
      switchMap(id => forkJoin({
        tipo: this.tipoHabitacionService.obtenerPorId(id),
        habitaciones: this.habitacionService.obtenerPorTipoHabitacionId(id)
      }))
    ).subscribe({
      next: ({ tipo, habitaciones }) => {
        this.tipo = tipo;
        this.habitaciones = habitaciones;
        this.precioDesde = habitaciones.length > 0
          ? Math.min(...habitaciones.map(h => h.precio))
          : null;
        this.habitacionesDisponibles = habitaciones.filter(h => h.estado === 'DISPONIBLE').length;
      },
      error: () => {
        this.router.navigate(['/']);
      }
    });
  }
}
