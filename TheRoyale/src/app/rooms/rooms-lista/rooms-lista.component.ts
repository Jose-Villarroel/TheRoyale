import { Component, inject, OnInit } from '@angular/core';
import { forkJoin } from 'rxjs';
import { CabeceraComponent } from '../../shared/cabecera/cabecera.component';
import { MenuMovilComponent } from '../../shared/menu-movil/menu-movil.component';
import { PiePaginaComponent } from '../../shared/pie-pagina/pie-pagina.component';
import { HeroInteriorComponent } from '../../shared/hero-interior/hero-interior.component';
import { CtaInteriorComponent } from '../../shared/cta-interior/cta-interior.component';
import { RoomListaItemComponent } from './secciones/room-lista-item/room-lista-item.component';
import { TipoHabitacionService } from '../../services/tipo-habitacion.service';
import { HabitacionService } from '../../services/habitacion.service';
import { TipoHabitacion } from '../../models/tipo-habitacion.model';

// Listado público de tipos de habitación (/rooms), el rooms-lista.html del backend.
// Hace los mismos cálculos que RoomController.listarRooms: precio mínimo y
// habitaciones disponibles por tipo.
@Component({
  selector: 'app-rooms-lista',
  standalone: true,
  imports: [
    CabeceraComponent,
    MenuMovilComponent,
    PiePaginaComponent,
    HeroInteriorComponent,
    CtaInteriorComponent,
    RoomListaItemComponent
  ],
  templateUrl: './rooms-lista.component.html'
})
export class RoomsListaComponent implements OnInit {
  private tipoHabitacionService = inject(TipoHabitacionService);
  private habitacionService = inject(HabitacionService);

  tipos: TipoHabitacion[] = [];
  // Indexados por id del tipo; si un tipo no aparece es que no tiene habitaciones
  preciosDesde: Partial<Record<number, number>> = {};
  disponibles: Partial<Record<number, number>> = {};
  menuAbierto = false;

  ngOnInit(): void {
    forkJoin({
      tipos: this.tipoHabitacionService.obtenerTodos(),
      habitaciones: this.habitacionService.obtenerTodos()
    }).subscribe(({ tipos, habitaciones }) => {
      this.tipos = tipos;
      for (const habitacion of habitaciones) {
        const tipoId = habitacion.tipoHabitacionId;
        const actual = this.preciosDesde[tipoId];
        this.preciosDesde[tipoId] = actual === undefined ? habitacion.precio : Math.min(actual, habitacion.precio);
        if (habitacion.estado === 'DISPONIBLE') {
          this.disponibles[tipoId] = (this.disponibles[tipoId] ?? 0) + 1;
        }
      }
    });
  }
}
