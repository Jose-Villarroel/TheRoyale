import { Component, inject, OnInit } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CabeceraComponent } from '../../shared/cabecera/cabecera.component';
import { MenuMovilComponent } from '../../shared/menu-movil/menu-movil.component';
import { PiePaginaComponent } from '../../shared/pie-pagina/pie-pagina.component';
import { HeroInteriorComponent } from '../../shared/hero-interior/hero-interior.component';
import { AlertaErrorComponent } from '../../shared/alerta-error/alerta-error.component';
import { TarjetaMiReservaComponent } from './secciones/tarjeta-mi-reserva/tarjeta-mi-reserva.component';
import { ReservaService } from '../../services/reserva.service';
import { AuthService } from '../../services/auth.service';
import { Reserva } from '../../models/reserva.model';

// Portal del huésped: reservas vigentes (AC16) e historial (AC17), con cancelar
// y modificar (AC18). El id del cliente sale de la sesión, no de la URL.
@Component({
  selector: 'app-mis-reservas',
  standalone: true,
  imports: [
    RouterLink,
    CabeceraComponent,
    MenuMovilComponent,
    PiePaginaComponent,
    HeroInteriorComponent,
    AlertaErrorComponent,
    TarjetaMiReservaComponent
  ],
  templateUrl: './mis-reservas.component.html'
})
export class MisReservasComponent implements OnInit {
  private reservaService = inject(ReservaService);
  private authService = inject(AuthService);
  private router = inject(Router);

  reservas: Reserva[] = [];
  verActivas = true;
  cargando = false;
  error: string | null = null;
  mensaje: string | null = null;
  menuAbierto = false;

  ngOnInit(): void {
    this.cargar();
  }

  cambiarPestana(activas: boolean): void {
    this.verActivas = activas;
    this.cargar();
  }

  modificar(reserva: Reserva): void {
    // El formulario de reserva entra en modo edición al recibir ?reservaId=
    this.router.navigate(['/book'], {
      queryParams: {
        reservaId: reserva.id,
        fechaInicio: reserva.fechaInicio,
        fechaFin: reserva.fechaFin,
        huespedes: reserva.cantidadPersonas,
        tipoId: reserva.tipoHabitacionId
      }
    });
  }

  cancelar(reserva: Reserva): void {
    if (!confirm(`Cancel reservation #${reserva.id}? This action cannot be undone.`)) {
      return;
    }

    this.mensaje = null;
    this.error = null;
    this.reservaService.cancelar(reserva.id).subscribe({
      next: () => {
        this.mensaje = `Reservation #${reserva.id} was cancelled.`;
        this.cargar();
      },
      // 409 si la llegada ya pasó o la cuenta tiene saldo pendiente
      error: (err) => this.error = err.error?.message || err.message || 'Error cancelling the reservation'
    });
  }

  // Botón "Sign Out" del hero, como en reservations.html
  cerrarSesion(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  private cargar(): void {
    const usuario = this.authService.usuario;
    if (!usuario) {
      this.router.navigate(['/login']);
      return;
    }

    this.cargando = true;
    this.reservaService.obtenerPorCliente(usuario.id, this.verActivas).subscribe({
      next: (reservas) => {
        this.reservas = reservas;
        this.cargando = false;
      },
      error: (err) => {
        this.cargando = false;
        this.error = err.error?.message || err.message || 'Error loading your reservations';
      }
    });
  }
}
