import { Component, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { CabeceraComponent } from '../../shared/cabecera/cabecera.component';
import { MenuMovilComponent } from '../../shared/menu-movil/menu-movil.component';
import { PiePaginaComponent } from '../../shared/pie-pagina/pie-pagina.component';
import { HeroInteriorComponent } from '../../shared/hero-interior/hero-interior.component';
import { AlertaErrorComponent } from '../../admin/secciones/alerta-error/alerta-error.component';
import { NavOperadorComponent } from '../secciones/nav-operador/nav-operador.component';
import { ReservaService } from '../../services/reserva.service';
import { EstadoReserva, Reserva } from '../../models/reserva.model';

// Todas las reservas del hotel con filtro por estado (AC21), desde el portal del operador.
@Component({
  selector: 'app-reservas-operador',
  standalone: true,
  imports: [
    RouterLink,
    DatePipe,
    CabeceraComponent,
    MenuMovilComponent,
    PiePaginaComponent,
    HeroInteriorComponent,
    AlertaErrorComponent,
    NavOperadorComponent
  ],
  templateUrl: './reservas-operador.component.html'
})
export class ReservasOperadorComponent implements OnInit {
  private reservaService = inject(ReservaService);

  readonly filtros: { etiqueta: string; estado: EstadoReserva | null }[] = [
    { etiqueta: 'All', estado: null },
    { etiqueta: 'Pending', estado: 'PENDIENTE' },
    { etiqueta: 'Confirmed', estado: 'CONFIRMADA' },
    { etiqueta: 'In Progress', estado: 'EN_CURSO' },
    { etiqueta: 'Cancelled', estado: 'CANCELADA' },
    { etiqueta: 'Completed', estado: 'FINALIZADA' }
  ];

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
