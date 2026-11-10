import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CabeceraComponent } from '../../shared/cabecera/cabecera.component';
import { MenuMovilComponent } from '../../shared/menu-movil/menu-movil.component';
import { PiePaginaComponent } from '../../shared/pie-pagina/pie-pagina.component';
import { HeroInteriorComponent } from '../../shared/hero-interior/hero-interior.component';
import { AlertaErrorComponent } from '../../shared/alerta-error/alerta-error.component';
import { NavAdminComponent } from '../secciones/nav-admin/nav-admin.component';
import { DashboardService } from '../../services/dashboard.service';
import { DashboardMetricas } from '../../models/dashboard.model';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    CabeceraComponent,
    MenuMovilComponent,
    PiePaginaComponent,
    HeroInteriorComponent,
    AlertaErrorComponent,
    NavAdminComponent
  ],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class AdminDashboardComponent implements OnInit {
  private dashboardService = inject(DashboardService);

  metricas: DashboardMetricas | null = null;
  cargando = true;
  error: string | null = null;
  menuAbierto = false;
  fechaActual = new Date();

  // Para el cálculo de la escala de la gráfica de barras
  maxIngresoMensual = 1;

  ngOnInit(): void {
    this.cargarMetricas();
  }

  cargarMetricas(): void {
    this.cargando = true;
    this.error = null;
    this.dashboardService.obtenerMetricas().subscribe({
      next: (datos) => {
        this.metricas = datos;
        this.calcularEscalas(datos);
        this.cargando = false;
      },
      error: (err) => {
        this.error = err.error?.message || err.message || 'Error loading dashboard metrics';
        this.cargando = false;
      }
    });
  }

  private calcularEscalas(datos: DashboardMetricas): void {
    if (datos.ingresosMensuales && datos.ingresosMensuales.length > 0) {
      const max = Math.max(...datos.ingresosMensuales.map(m => m.total));
      this.maxIngresoMensual = max > 0 ? max * 1.15 : 1000;
    }
  }

  obtenerPorcentajeBarra(monto: number): number {
    if (!this.maxIngresoMensual || this.maxIngresoMensual <= 0) return 0;
    const pct = (monto / this.maxIngresoMensual) * 100;
    return Math.min(Math.max(pct, 4), 100);
  }

  claseEstado(estado: string): string {
    switch (estado) {
      case 'CONFIRMADA': return 'badge-confirmada';
      case 'EN_CURSO': return 'badge-en-curso';
      case 'FINALIZADA': return 'badge-finalizada';
      case 'CANCELADA': return 'badge-cancelada';
      default: return 'badge-pendiente';
    }
  }
}
