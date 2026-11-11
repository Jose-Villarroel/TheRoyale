import { Component, inject, OnInit } from '@angular/core';
import { CabeceraComponent } from '../../shared/cabecera/cabecera.component';
import { MenuMovilComponent } from '../../shared/menu-movil/menu-movil.component';
import { PiePaginaComponent } from '../../shared/pie-pagina/pie-pagina.component';
import { HeroInteriorComponent } from '../../shared/hero-interior/hero-interior.component';
import { AlertaErrorComponent } from '../../shared/alerta-error/alerta-error.component';
import { NavOperadorComponent } from '../secciones/nav-operador/nav-operador.component';
import { FiltrosEstadoComponent } from '../../shared/filtros-estado/filtros-estado.component';
import { TablaReservasOperadorComponent } from './secciones/tabla-reservas-operador/tabla-reservas-operador.component';
import { ReservaService } from '../../services/reserva.service';
import { EstadoReserva, Reserva } from '../../models/reserva.model';

// Todas las reservas del hotel con filtro por estado (AC21), desde el portal del operador.
// La página solo carga datos; filtros y tabla son componentes de presentación.
@Component({
  selector: 'app-reservas-operador',
  standalone: true,
  imports: [
    CabeceraComponent,
    MenuMovilComponent,
    PiePaginaComponent,
    HeroInteriorComponent,
    AlertaErrorComponent,
    NavOperadorComponent,
    FiltrosEstadoComponent,
    TablaReservasOperadorComponent
  ],
  templateUrl: './reservas-operador.component.html'
})
export class ReservasOperadorComponent implements OnInit {
  private reservaService = inject(ReservaService);

  reservas: Reserva[] = [];
  estadoActivo: EstadoReserva | null = null;
  error: string | null = null;
  menuAbierto = false;

  ngOnInit(): void {
    this.cargar();
  }

  filtrar(estado: EstadoReserva | null): void {
    this.estadoActivo = estado;
    this.cargar();
  }

  private cargar(): void {
    this.reservaService.obtenerTodas(this.estadoActivo).subscribe({
      next: (reservas) => {
        this.reservas = reservas;
        this.error = null;
      },
      error: (err) => this.error = err.error?.message || err.message || 'Error loading reservations'
    });
  }
}
