import { Component, inject, OnInit } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CabeceraComponent } from '../../shared/cabecera/cabecera.component';
import { MenuMovilComponent } from '../../shared/menu-movil/menu-movil.component';
import { PiePaginaComponent } from '../../shared/pie-pagina/pie-pagina.component';
import { HeroInteriorComponent } from '../../shared/hero-interior/hero-interior.component';
import { CtaInteriorComponent } from '../../shared/cta-interior/cta-interior.component';
import { AlertaErrorComponent } from '../secciones/alerta-error/alerta-error.component';
import { NavAdminComponent } from '../secciones/nav-admin/nav-admin.component';
import { ReservaService } from '../../services/reserva.service';
import { Reserva } from '../../models/reserva.model';

@Component({
  selector: 'app-reserva-detalle',
  standalone: true,
  imports: [
    CurrencyPipe,
    DatePipe,
    RouterLink,
    CabeceraComponent,
    MenuMovilComponent,
    PiePaginaComponent,
    HeroInteriorComponent,
    CtaInteriorComponent,
    AlertaErrorComponent,
    NavAdminComponent
  ],
  templateUrl: './reserva-detalle.component.html'
})
export class ReservaDetalleComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private reservaService = inject(ReservaService);

  id = Number(this.route.snapshot.paramMap.get('id'));
  reserva: Reserva | null = null;
  error: string | null = null;
  menuAbierto = false;

  ngOnInit(): void {
    this.reservaService.obtenerPorId(this.id).subscribe({
      next: (reserva) => this.reserva = reserva,
      // 404 si la reserva no existe
      error: (err) => this.error = err.error?.message || err.message || 'Error loading reservation'
    });
  }

  get noches(): number {
    if (!this.reserva) {
      return 0;
    }
    const ms = new Date(`${this.reserva.fechaFin}T00:00:00`).getTime()
             - new Date(`${this.reserva.fechaInicio}T00:00:00`).getTime();
    return Math.round(ms / 86400000);
  }

  get totalEstimado(): number {
    return (this.reserva?.precioNocheAcordado ?? 0) * this.noches;
  }
}
