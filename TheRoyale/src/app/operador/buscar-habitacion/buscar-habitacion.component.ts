import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { ReservaService } from '../../services/reserva.service';
import { Reserva } from '../../models/reserva.model';
import { CabeceraComponent } from '../../shared/cabecera/cabecera.component';
import { MenuMovilComponent } from '../../shared/menu-movil/menu-movil.component';
import { PiePaginaComponent } from '../../shared/pie-pagina/pie-pagina.component';
import { HeroInteriorComponent } from '../../shared/hero-interior/hero-interior.component';
import { AlertaErrorComponent } from '../../shared/alerta-error/alerta-error.component';
import { NavOperadorComponent } from '../secciones/nav-operador/nav-operador.component';
import { FormBuscarHabitacionComponent } from './secciones/form-buscar-habitacion/form-buscar-habitacion.component';
import { TarjetaClienteEncontradoComponent } from './secciones/tarjeta-cliente-encontrado/tarjeta-cliente-encontrado.component';

// El operador ingresa el numero de habitacion para localizar al huesped,
// ver su informacion y confirmar que sea el correcto antes de gestionar servicios.
// La página hace la búsqueda; el formulario y la tarjeta del huésped son secciones/.
@Component({
  selector: 'app-buscar-habitacion',
  standalone: true,
  imports: [
    CabeceraComponent,
    MenuMovilComponent,
    PiePaginaComponent,
    HeroInteriorComponent,
    AlertaErrorComponent,
    NavOperadorComponent,
    FormBuscarHabitacionComponent,
    TarjetaClienteEncontradoComponent
  ],
  templateUrl: './buscar-habitacion.component.html'
})
export class BuscarHabitacionComponent {
  private reservaService = inject(ReservaService);
  private router = inject(Router);

  numeroHabitacion = '';
  buscando = false;
  reserva: Reserva | null = null;
  error: string | null = null;
  menuAbierto = false;

  buscar(): void {
    const numero = this.numeroHabitacion.trim();
    if (!numero) return;

    this.buscando = true;
    this.reserva = null;
    this.error = null;

    this.reservaService.buscarPorHabitacion(numero).subscribe({
      next: (reserva) => {
        this.buscando = false;
        this.reserva = reserva;
      },
      error: (err) => {
        this.buscando = false;
        // 404 = no hay reserva activa para ese numero; muestra mensaje amigable
        this.error = err.status === 404
          ? `No active reservation found for room ${numero}.`
          : (err.error?.message || 'Error searching for room.');
      }
    });
  }

  // Lleva al detalle de la reserva para agregar consumos / gestionar la cuenta
  irAReserva(): void {
    if (this.reserva) {
      this.router.navigate(['/operator/reservas', this.reserva.id]);
    }
  }

  limpiar(): void {
    this.numeroHabitacion = '';
    this.reserva = null;
    this.error = null;
  }
}
