import { Component, inject, OnInit } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { CabeceraComponent } from '../../shared/cabecera/cabecera.component';
import { MenuMovilComponent } from '../../shared/menu-movil/menu-movil.component';
import { PiePaginaComponent } from '../../shared/pie-pagina/pie-pagina.component';
import { HeroInteriorComponent } from '../../shared/hero-interior/hero-interior.component';
import { AlertaErrorComponent } from '../../admin/secciones/alerta-error/alerta-error.component';
import { NavOperadorComponent } from '../secciones/nav-operador/nav-operador.component';
import { ReservaService } from '../../services/reserva.service';
import { CuentaService } from '../../services/cuenta.service';
import { ServicioService } from '../../services/servicio.service';
import { AuthService } from '../../services/auth.service';
import { Reserva } from '../../models/reserva.model';
import { Cuenta } from '../../models/cuenta.model';
import { Servicio } from '../../models/servicio.model';

// Pantalla central del Feature 07: datos del huésped y la habitación (AC22), cuenta de
// consumos (AC23, AC24), pagos (AC25) y las transiciones del ciclo de vida de la reserva.
// Equivale a operator/reserva-detalle.html, que sigue sirviendo a la versión Thymeleaf.
@Component({
  selector: 'app-reserva-detalle-operador',
  standalone: true,
  imports: [
    CurrencyPipe,
    DatePipe,
    ReactiveFormsModule,
    RouterLink,
    CabeceraComponent,
    MenuMovilComponent,
    PiePaginaComponent,
    HeroInteriorComponent,
    AlertaErrorComponent,
    NavOperadorComponent
  ],
  templateUrl: './reserva-detalle-operador.component.html'
})
export class ReservaDetalleOperadorComponent implements OnInit {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private reservaService = inject(ReservaService);
  private cuentaService = inject(CuentaService);
  private servicioService = inject(ServicioService);
  private authService = inject(AuthService);

  // Los mismos métodos que acepta OperatorController en la versión Thymeleaf
  readonly metodosPago = ['CASH', 'CARD', 'TRANSFER'];

  id = Number(this.route.snapshot.paramMap.get('id'));
  reserva: Reserva | null = null;
  cuenta: Cuenta | null = null;
  servicios: Servicio[] = [];
  error: string | null = null;
  mensaje: string | null = null;
  menuAbierto = false;

  consumoForm = this.fb.nonNullable.group({
    servicioId: [null as number | null, [Validators.required]],
    cantidad: [1, [Validators.required, Validators.min(1)]]
  });

  pagoForm = this.fb.nonNullable.group({
    monto: [null as number | null, [Validators.required, Validators.min(0.01)]],
    metodoPago: ['CARD', [Validators.required]]
  });

  ngOnInit(): void {
    this.cargarReserva();
    this.cargarCuenta();
    this.servicioService.obtenerTodos().subscribe(servicios => this.servicios = servicios);
  }

  get noches(): number {
    if (!this.reserva) {
      return 0;
    }
    const ms = new Date(this.reserva.fechaFin + 'T00:00:00').getTime()
             - new Date(this.reserva.fechaInicio + 'T00:00:00').getTime();
    return Math.round(ms / 86400000);
  }

  get totalEstadia(): number {
    return (this.reserva?.precioNocheAcordado ?? 0) * this.noches;
  }

  // Solo una cuenta abierta admite cargos y pagos; el backend lo vuelve a comprobar
  get cuentaOperable(): boolean {
    return this.cuenta?.estado === 'ABIERTA';
  }

  get puedeConfirmar(): boolean {
    return this.reserva?.estado === 'PENDIENTE';
  }

  // El backend solo deja cancelar si la llegada es posterior a hoy (AC22)
  get puedeCancelar(): boolean {
    if (!this.reserva) {
      return false;
    }
    const vigente = this.reserva.estado === 'PENDIENTE' || this.reserva.estado === 'CONFIRMADA';
    return vigente && this.reserva.fechaInicio > new Date().toISOString().slice(0, 10);
  }

  get puedeCheckIn(): boolean {
    return this.reserva?.estado === 'CONFIRMADA';
  }

  get puedeCheckOut(): boolean {
    return this.reserva?.estado === 'EN_CURSO';
  }

  confirmar(): void {
    this.ejecutarTransicion(this.reservaService.confirmar(this.id), 'Reservation confirmed.');
  }

  cancelar(): void {
    if (confirm('Cancel reservation #' + this.id + '?')) {
      this.ejecutarTransicion(this.reservaService.cancelar(this.id), 'Reservation cancelled.');
    }
  }

  checkIn(): void {
    this.ejecutarTransicion(this.reservaService.checkIn(this.id),
      'Check-in completed. The guest account is open.');
  }

  checkOut(): void {
    this.ejecutarTransicion(this.reservaService.checkOut(this.id),
      'Check-out completed. The room is available again.');
  }

  agregarConsumo(): void {
    if (this.consumoForm.invalid) {
      this.consumoForm.markAllAsTouched();
      return;
    }

    const { servicioId, cantidad } = this.consumoForm.getRawValue();
    this.limpiarAvisos();
    this.cuentaService.agregarConsumo(this.id, {
      servicioId: servicioId!,
      cantidad: cantidad,
      operadorId: this.authService.usuario?.id ?? null
    }).subscribe({
      next: (cuenta) => {
        this.cuenta = cuenta;
        this.mensaje = 'Charge added to the account.';
        this.consumoForm.reset({ servicioId: null, cantidad: 1 });
      },
      error: (err) => this.error = this.mensajeDe(err, 'Error adding the charge')
    });
  }

  eliminarConsumo(itemId: number): void {
    if (!confirm('Remove this charge from the account?')) {
      return;
    }

    this.limpiarAvisos();
    this.cuentaService.eliminarConsumo(this.id, itemId).subscribe({
      next: (cuenta) => {
        this.cuenta = cuenta;
        this.mensaje = 'Charge removed from the account.';
      },
      // 409 si el cargo ya estaba pagado o si quitarlo dejaría la cuenta en negativo
      error: (err) => this.error = this.mensajeDe(err, 'Error removing the charge')
    });
  }

  registrarPago(): void {
    if (this.pagoForm.invalid) {
      this.pagoForm.markAllAsTouched();
      return;
    }

    const { monto, metodoPago } = this.pagoForm.getRawValue();
    this.limpiarAvisos();
    this.cuentaService.registrarPago(this.id, {
      monto: monto!,
      metodoPago: metodoPago,
      operadorId: this.authService.usuario?.id ?? null
    }).subscribe({
      next: (cuenta) => {
        this.cuenta = cuenta;
        this.mensaje = 'Payment registered.';
        this.pagoForm.reset({ monto: null, metodoPago: 'CARD' });
      },
      error: (err) => this.error = this.mensajeDe(err, 'Error registering the payment')
    });
  }

  // Cobra el saldo restante de una sola vez: el caso habitual antes del check-out
  pagarSaldoCompleto(): void {
    if (this.cuenta && this.cuenta.saldo > 0) {
      this.pagoForm.patchValue({ monto: this.cuenta.saldo });
    }
  }

  private ejecutarTransicion(peticion: Observable<Reserva>, exito: string): void {
    this.limpiarAvisos();
    peticion.subscribe({
      next: (reserva) => {
        this.reserva = reserva;
        this.mensaje = exito;
        // Confirmar y hacer check-in abren la cuenta; el check-out la cierra
        this.cargarCuenta();
      },
      error: (err) => this.error = this.mensajeDe(err, 'The operation could not be completed')
    });
  }

  private cargarReserva(): void {
    this.reservaService.obtenerPorId(this.id).subscribe({
      next: (reserva) => this.reserva = reserva,
      error: (err) => this.error = this.mensajeDe(err, 'Error loading reservation')
    });
  }

  private cargarCuenta(): void {
    this.cuentaService.obtenerPorReserva(this.id).subscribe({
      next: (cuenta) => this.cuenta = cuenta,
      // 404 mientras la reserva siga PENDIENTE: la cuenta nace al confirmarla
      error: () => this.cuenta = null
    });
  }

  private limpiarAvisos(): void {
    this.mensaje = null;
    this.error = null;
  }

  private mensajeDe(err: { error?: { message?: string }; message?: string }, porDefecto: string): string {
    return err.error?.message || err.message || porDefecto;
  }
}
