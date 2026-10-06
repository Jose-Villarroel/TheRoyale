import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
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

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    const tipo = this.tipoHabitacionService.obtenerPorId(id);
    if (!tipo) {
      this.router.navigate(['/']);
      return;
    }

    this.tipo = tipo;
    this.habitaciones = this.habitacionService.obtenerPorTipoHabitacionId(id);
    this.precioDesde = this.habitaciones.length > 0
      ? Math.min(...this.habitaciones.map(h => h.precio))
      : null;
    this.habitacionesDisponibles = this.habitaciones.filter(h => h.estado === 'DISPONIBLE').length;
  }
}
