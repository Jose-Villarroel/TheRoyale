import { Component, inject, OnInit } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { catchError, forkJoin, of } from 'rxjs';
import { CabeceraComponent } from '../../shared/cabecera/cabecera.component';
import { MenuMovilComponent } from '../../shared/menu-movil/menu-movil.component';
import { PiePaginaComponent } from '../../shared/pie-pagina/pie-pagina.component';
import { HeroInteriorComponent } from '../../shared/hero-interior/hero-interior.component';
import { AlertaErrorComponent } from '../../shared/alerta-error/alerta-error.component';
import { TarjetaMiReservaComponent } from './secciones/tarjeta-mi-reserva/tarjeta-mi-reserva.component';
import { FormResenaComponent } from './secciones/form-resena/form-resena.component';
import { ReservaService } from '../../services/reserva.service';
import { ResenaService } from '../../services/resena.service';
import { AuthService } from '../../services/auth.service';
import { Reserva } from '../../models/reserva.model';
import { DatosResena, Resena } from '../../models/resena.model';

// Portal del huésped: reservas vigentes (AC16) e historial (AC17), con cancelar
// y modificar (AC18), y reseñas de las estadías FINALIZADA (crear, editar y eliminar).
// El id del cliente sale de la sesión, no de la URL.
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
    TarjetaMiReservaComponent,
    FormResenaComponent
  ],
  templateUrl: './mis-reservas.component.html'
})
export class MisReservasComponent implements OnInit {
  private reservaService = inject(ReservaService);
  private resenaService = inject(ResenaService);
  private authService = inject(AuthService);
  private router = inject(Router);

  reservas: Reserva[] = [];
  // Reseñas del huésped (incluidas las ocultas por el hotel) indexadas por id de reserva
  resenasPorReserva: Partial<Record<number, Resena>> = {};
  verActivas = true;
  cargando = false;
  error: string | null = null;
  mensaje: string | null = null;
  menuAbierto = false;

  // Formulario de reseña abierto: la reserva que se reseña y, si ya existe, su reseña
  reservaResenando: Reserva | null = null;
  resenaEditando: Resena | null = null;
  errorResena: string | null = null;
  enviandoResena = false;

  ngOnInit(): void {
    this.cargar();
  }

  cambiarPestana(activas: boolean): void {
    this.verActivas = activas;
    this.cerrarFormularioResena();
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

  // ===== Reseñas =====
  resenar(reserva: Reserva): void {
    this.abrirFormularioResena(reserva, null);
  }

  editarResena(reserva: Reserva): void {
    const resena = this.resenasPorReserva[reserva.id];
    if (!resena) {
      this.error = 'We could not load your review. Please try again.';
      return;
    }
    this.abrirFormularioResena(reserva, resena);
  }

  enviarResena(datos: DatosResena): void {
    const usuario = this.authService.usuario;
    const reserva = this.reservaResenando;
    if (!usuario || !reserva) {
      return;
    }

    const resena = this.resenaEditando;
    const peticion = resena
      ? this.resenaService.actualizar(resena.id, { clienteId: usuario.id, ...datos })
      : this.resenaService.crear({ reservaId: reserva.id, clienteId: usuario.id, ...datos });

    this.enviandoResena = true;
    this.errorResena = null;
    peticion.subscribe({
      next: () => {
        this.enviandoResena = false;
        this.mensaje = resena ? 'Your review was updated.' : 'Thank you! Your review was published.';
        this.cerrarFormularioResena();
        this.cargar();
      },
      // 400 validación, 403 reserva ajena, 409 estado no FINALIZADA o reseña duplicada
      error: (err) => {
        this.enviandoResena = false;
        this.errorResena = err.error?.message || err.message || 'Error saving your review';
      }
    });
  }

  eliminarResena(): void {
    const usuario = this.authService.usuario;
    const resena = this.resenaEditando;
    if (!usuario || !resena) {
      return;
    }
    if (!confirm('Delete your review? This action cannot be undone.')) {
      return;
    }

    this.enviandoResena = true;
    this.errorResena = null;
    this.resenaService.eliminar(resena.id, usuario.id).subscribe({
      next: () => {
        this.enviandoResena = false;
        this.mensaje = 'Your review was deleted.';
        this.cerrarFormularioResena();
        this.cargar();
      },
      error: (err) => {
        this.enviandoResena = false;
        this.errorResena = err.error?.message || err.message || 'Error deleting your review';
      }
    });
  }

  cerrarFormularioResena(): void {
    this.reservaResenando = null;
    this.resenaEditando = null;
    this.errorResena = null;
  }

  // Botón "Sign Out" del hero, como en reservations.html
  cerrarSesion(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  private abrirFormularioResena(reserva: Reserva, resena: Resena | null): void {
    this.mensaje = null;
    this.error = null;
    this.errorResena = null;
    this.reservaResenando = reserva;
    this.resenaEditando = resena;
    // El formulario se pinta encima del grid: se baja hasta él cuando ya está en el DOM
    setTimeout(() => document.getElementById('form-resena')?.scrollIntoView({ behavior: 'smooth', block: 'center' }));
  }

  private cargar(): void {
    const usuario = this.authService.usuario;
    if (!usuario) {
      this.router.navigate(['/login']);
      return;
    }

    this.cargando = true;
    // Si las reseñas fallan, las reservas se listan igual (sin las estrellas dadas)
    forkJoin({
      reservas: this.reservaService.obtenerPorCliente(usuario.id, this.verActivas),
      resenas: this.resenaService.obtenerPorCliente(usuario.id).pipe(catchError(() => of([] as Resena[])))
    }).subscribe({
      next: ({ reservas, resenas }) => {
        this.reservas = reservas;
        this.resenasPorReserva = {};
        for (const resena of resenas) {
          this.resenasPorReserva[resena.reservaId] = resena;
        }
        this.cargando = false;
      },
      error: (err) => {
        this.cargando = false;
        this.error = err.error?.message || err.message || 'Error loading your reservations';
      }
    });
  }
}
