import { Component, inject, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable } from 'rxjs';
import { CabeceraComponent } from '../../shared/cabecera/cabecera.component';
import { MenuMovilComponent } from '../../shared/menu-movil/menu-movil.component';
import { PiePaginaComponent } from '../../shared/pie-pagina/pie-pagina.component';
import { HeroInteriorComponent } from '../../shared/hero-interior/hero-interior.component';
import { AlertaErrorComponent } from '../../shared/alerta-error/alerta-error.component';
import { NavOperadorComponent } from '../secciones/nav-operador/nav-operador.component';
import { TarjetaEstadiaComponent } from './secciones/tarjeta-estadia/tarjeta-estadia.component';
import { AccionesReservaComponent } from './secciones/acciones-reserva/acciones-reserva.component';
import { TarjetaHuespedComponent } from './secciones/tarjeta-huesped/tarjeta-huesped.component';
import { TarjetaHabitacionAsignadaComponent } from './secciones/tarjeta-habitacion-asignada/tarjeta-habitacion-asignada.component';
import { CuentaResumenComponent } from './secciones/cuenta-resumen/cuenta-resumen.component';
import { TablaConsumosComponent } from './secciones/tabla-consumos/tabla-consumos.component';
import { TablaPagosComponent } from './secciones/tabla-pagos/tabla-pagos.component';
import { FormConsumoComponent } from './secciones/form-consumo/form-consumo.component';
import { FormPagoComponent } from './secciones/form-pago/form-pago.component';
import { ReservaService } from '../../services/reserva.service';
import { CuentaService } from '../../services/cuenta.service';
import { ServicioService } from '../../services/servicio.service';
import { AuthService } from '../../services/auth.service';
import { Reserva } from '../../models/reserva.model';
import { Cuenta } from '../../models/cuenta.model';
import { Servicio } from '../../models/servicio.model';
import { noches, puedeCancelar } from '../../utils/fechas';

// Pantalla central del Feature 07: datos del huésped y la habitación (AC22), cuenta de
// consumos (AC23, AC24), pagos (AC25) y las transiciones del ciclo de vida de la reserva.
// Equivale a operator/reserva-detalle.html, que sigue sirviendo a la versión Thymeleaf.
// La página guarda el estado y llama a los servicios; cada bloque visual vive en secciones/.
@Component({
  selector: 'app-reserva-detalle-operador',
  standalone: true,
  imports: [
    CabeceraComponent,
    MenuMovilComponent,
    PiePaginaComponent,
    HeroInteriorComponent,
    AlertaErrorComponent,
    NavOperadorComponent,
    TarjetaEstadiaComponent,
    AccionesReservaComponent,
    TarjetaHuespedComponent,
    TarjetaHabitacionAsignadaComponent,
    CuentaResumenComponent,
    TablaConsumosComponent,
    TablaPagosComponent,
    FormConsumoComponent,
    FormPagoComponent
  ],
  providers: [DatePipe],
  templateUrl: './reserva-detalle-operador.component.html'
})
export class ReservaDetalleOperadorComponent implements OnInit {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private reservaService = inject(ReservaService);
  private cuentaService = inject(CuentaService);
  private servicioService = inject(ServicioService);
  private authService = inject(AuthService);
  private datePipe = inject(DatePipe);

  // Los mismos métodos que acepta OperatorController en la versión Thymeleaf
  readonly metodosPago = ['CASH', 'CARD', 'TRANSFER'] as const;

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

  // Como en Thymeleaf: "Huésped · Room 101 · llegada → salida"
  get subtituloHero(): string {
    if (!this.reserva) {
      return 'Guest details, stay account and the actions available at this point of the stay.';
    }
    const r = this.reserva;
    const fecha = (valor: string) => this.datePipe.transform(valor, 'mediumDate');
    return `${r.clienteNombre} ${r.clienteApellido} · Room ${r.habitacionNumero} · ${fecha(r.fechaInicio)} → ${fecha(r.fechaFin)}`;
  }

  get noches(): number {
    return this.reserva ? noches(this.reserva.fechaInicio, this.reserva.fechaFin) : 0;
  }

  get totalEstadia(): number {
    return (this.reserva?.precioNocheAcordado ?? 0) * this.noches;
  }

  // Solo una cuenta abierta admite cargos y pagos; el backend lo vuelve a comprobar
  get cuentaOperable(): boolean {
    return this.cuenta?.estado === 'ABIERTA';
  }

  // Saldo en cero tras haber pagado algo: la cuenta quedó al día
  get cuentaSaldada(): boolean {
    return !!this.cuenta && this.cuenta.saldo === 0 && this.cuenta.pagos.length > 0;
  }

  get puedeConfirmar(): boolean {
    return this.reserva?.estado === 'PENDIENTE';
  }

  // El backend solo deja cancelar si la llegada es posterior a hoy (AC22)
  get puedeCancelar(): boolean {
    return !!this.reserva && puedeCancelar(this.reserva);
  }

  // Con el huésped dentro (EN_CURSO) el backend responde 409
  get puedeEliminar(): boolean {
    return !!this.reserva && this.reserva.estado !== 'EN_CURSO';
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

  eliminar(): void {
    if (!confirm('Delete reservation #' + this.id + '? This action cannot be undone.')) {
      return;
    }

    this.limpiarAvisos();
    this.reservaService.eliminar(this.id).subscribe({
      next: () => this.router.navigate(['/operator/reservas']),
      error: (err) => this.error = this.mensajeDe(err, 'Error deleting the reservation')
    });
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
