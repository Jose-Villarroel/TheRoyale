import { Component, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CabeceraComponent } from '../../shared/cabecera/cabecera.component';
import { MenuMovilComponent } from '../../shared/menu-movil/menu-movil.component';
import { PiePaginaComponent } from '../../shared/pie-pagina/pie-pagina.component';
import { HeroInteriorComponent } from '../../shared/hero-interior/hero-interior.component';
import { AlertaErrorComponent } from '../../shared/alerta-error/alerta-error.component';
import { NavAdminComponent } from '../secciones/nav-admin/nav-admin.component';
import { FiltrosEstadoComponent } from '../../shared/filtros-estado/filtros-estado.component';
import { GridReservasComponent } from './secciones/grid-reservas/grid-reservas.component';
import { ReservaService } from '../../services/reserva.service';
import { EstadoReserva, Reserva } from '../../models/reserva.model';

// Lista TODAS las reservas del hotel, no solo las del usuario autenticado.
// Los filtros por estado viven en app-filtros-estado (compartido con el portal del operador).
@Component({
  selector: 'app-reservas-lista',
  standalone: true,
  imports: [
    RouterLink,
    CabeceraComponent,
    MenuMovilComponent,
    PiePaginaComponent,
    HeroInteriorComponent,
    AlertaErrorComponent,
    NavAdminComponent,
    FiltrosEstadoComponent,
    GridReservasComponent
  ],
  templateUrl: './reservas-lista.component.html'
})
export class ReservasListaComponent implements OnInit {
  private reservaService = inject(ReservaService);

  reservas: Reserva[] = [];
  estadoActivo: EstadoReserva | null = null;
  error: string | null = null;
  menuAbierto = false;

  ngOnInit(): void {
    this.cargarReservas();
  }

  filtrar(estado: EstadoReserva | null): void {
    this.estadoActivo = estado;
    this.cargarReservas();
  }

  private cargarReservas(): void {
    this.reservaService.obtenerTodas(this.estadoActivo).subscribe({
      next: (reservas) => {
        this.reservas = reservas;
        this.error = null;
      },
      error: (err) => {
        this.error = err.error?.message || err.message || 'Error loading reservations';
      }
    });
  }
}
