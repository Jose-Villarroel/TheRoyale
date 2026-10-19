import { Component, inject, OnInit } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { of, switchMap } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { CabeceraComponent } from '../../shared/cabecera/cabecera.component';
import { MenuMovilComponent } from '../../shared/menu-movil/menu-movil.component';
import { PiePaginaComponent } from '../../shared/pie-pagina/pie-pagina.component';
import { HeroInteriorComponent } from '../../shared/hero-interior/hero-interior.component';
import { AlertaErrorComponent } from '../../admin/secciones/alerta-error/alerta-error.component';
import { GridDisponibilidadComponent } from './secciones/grid-disponibilidad/grid-disponibilidad.component';
import { DisponibilidadService } from '../../services/disponibilidad.service';
import { ReservaService } from '../../services/reserva.service';
import { AuthService } from '../../services/auth.service';
import { DisponibilidadTipo, Reserva } from '../../models/reserva.model';

// Reserva en tres pasos: fechas -> tipo de habitación -> confirmación.
// La habitación concreta no se elige aquí: la asigna el backend al crear la reserva.
@Component({
  selector: 'app-reserva-formulario',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    CurrencyPipe,
    DatePipe,
    CabeceraComponent,
    MenuMovilComponent,
    PiePaginaComponent,
    HeroInteriorComponent,
    AlertaErrorComponent,
    GridDisponibilidadComponent
  ],
  templateUrl: './reserva-formulario.component.html'
})
export class ReservaFormularioComponent implements OnInit {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private disponibilidadService = inject(DisponibilidadService);
  private reservaService = inject(ReservaService);
  authService = inject(AuthService);

  // Hoy en yyyy-MM-dd: tope inferior de los <input type="date">
  readonly hoy = this.aTexto(new Date());

  // Reserva recien creada: mientras exista, la pagina muestra la confirmacion en vez del formulario
  confirmada: Reserva | null = null;
  disponibilidad: DisponibilidadTipo[] = [];
  seleccionado: DisponibilidadTipo | null = null;
  // Tipo que venia en la url de la landing: solo una pista hasta que llegue la disponibilidad real
  private tipoPreferidoId: number | null = null;
  consultando = false;
  enviando = false;
  error: string | null = null;
  menuAbierto = false;

  reservaForm: FormGroup = this.fb.group({
    fechaInicio: ['', [Validators.required]],
    fechaFin: ['', [Validators.required]],
    cantidadPersonas: [2, [Validators.required, Validators.min(1)]]
  });

  ngOnInit(): void {
    this.precargarDesdeQueryParams();

    // switchMap descarta la respuesta anterior si el usuario sigue cambiando fechas
    this.reservaForm.valueChanges.pipe(
      switchMap(() => {
        if (!this.rangoValido) {
          this.disponibilidad = [];
          this.seleccionado = null;
          return of(null);
        }
        this.consultando = true;
        return this.disponibilidadService
          .consultar(this.reservaForm.value.fechaInicio, this.reservaForm.value.fechaFin)
          .pipe(catchError((err) => {
            this.error = err.error?.message || err.message || 'Error checking availability';
            return of(null);
          }));
      })
    ).subscribe((disponibilidad) => {
      this.consultando = false;
      if (disponibilidad === null) {
        return;
      }
      this.error = null;
      this.disponibilidad = disponibilidad;
      // El tipo elegido puede haberse agotado con las fechas nuevas
      const buscadoId = this.seleccionado?.tipoHabitacionId ?? this.tipoPreferidoId;
      this.tipoPreferidoId = null;
      this.seleccionado = disponibilidad.find(
        (tipo) => tipo.tipoHabitacionId === buscadoId && tipo.habitacionesDisponibles > 0) ?? null;
    });

    // Dispara la primera consulta si venimos de la landing con fechas ya puestas
    if (this.rangoValido) {
      this.reservaForm.updateValueAndValidity({ emitEvent: true });
    }
  }

  get rangoValido(): boolean {
    const { fechaInicio, fechaFin } = this.reservaForm.value;
    return !!fechaInicio && !!fechaFin && fechaFin > fechaInicio && fechaInicio >= this.hoy;
  }

  // La salida nunca puede ser el mismo día que la llegada
  get minimaSalida(): string {
    const inicio = this.reservaForm.value.fechaInicio;
    if (!inicio) {
      return this.hoy;
    }
    const siguiente = new Date(`${inicio}T00:00:00`);
    siguiente.setDate(siguiente.getDate() + 1);
    return this.aTexto(siguiente);
  }

  get noches(): number {
    if (!this.rangoValido) {
      return 0;
    }
    const { fechaInicio, fechaFin } = this.reservaForm.value;
    const ms = new Date(`${fechaFin}T00:00:00`).getTime() - new Date(`${fechaInicio}T00:00:00`).getTime();
    return Math.round(ms / 86400000);
  }

  get totalEstimado(): number {
    return (this.seleccionado?.precioDesde ?? 0) * this.noches;
  }

  get puedeConfirmar(): boolean {
    return this.reservaForm.valid && this.rangoValido && this.seleccionado !== null && !this.enviando;
  }

  // Vuelve a esta misma pagina con lo ya elegido: iniciar sesion no debe perder la busqueda
  get urlRetorno(): string {
    return this.router.createUrlTree(['/book'], {
      queryParams: {
        fechaInicio: this.reservaForm.value.fechaInicio || null,
        fechaFin: this.reservaForm.value.fechaFin || null,
        huespedes: this.reservaForm.value.cantidadPersonas,
        tipoId: this.seleccionado?.tipoHabitacionId ?? null
      }
    }).toString();
  }

  elegirTipo(tipo: DisponibilidadTipo): void {
    this.seleccionado = tipo;
  }

  confirmar(): void {
    const cliente = this.authService.usuario;
    if (!this.puedeConfirmar || !cliente) {
      return;
    }

    this.enviando = true;
    this.reservaService.crear({
      clienteId: cliente.id,
      tipoHabitacionId: this.seleccionado!.tipoHabitacionId,
      fechaInicio: this.reservaForm.value.fechaInicio,
      fechaFin: this.reservaForm.value.fechaFin,
      cantidadPersonas: Number(this.reservaForm.value.cantidadPersonas)
    }).subscribe({
      next: (reserva) => {
        this.enviando = false;
        // No se navega a ningun listado: la confirmacion se muestra aqui mismo
        this.confirmada = reserva;
        window.scrollTo({ top: 0, behavior: 'smooth' });
      },
      error: (err) => {
        this.enviando = false;
        // 409 cuando otra reserva ocupó la última habitación mientras se llenaba el formulario
        this.error = err.error?.message || err.message || 'Error creating reservation';
        this.refrescarDisponibilidad();
      }
    });
  }

  reservarOtra(): void {
    this.confirmada = null;
    this.seleccionado = null;
    this.reservaForm.reset({ fechaInicio: '', fechaFin: '', cantidadPersonas: 2 });
  }

  private refrescarDisponibilidad(): void {
    if (this.rangoValido) {
      this.reservaForm.updateValueAndValidity({ emitEvent: true });
    }
  }

  // La landing manda las fechas elegidas en el buscador como query params
  private precargarDesdeQueryParams(): void {
    const params = this.route.snapshot.queryParamMap;
    const fechaInicio = params.get('fechaInicio');
    const fechaFin = params.get('fechaFin');
    const huespedes = params.get('huespedes');

    this.reservaForm.patchValue({
      fechaInicio: fechaInicio && fechaInicio >= this.hoy ? fechaInicio : '',
      fechaFin: fechaFin ?? '',
      cantidadPersonas: huespedes ? Number(huespedes) : 2
    }, { emitEvent: false });

    const tipoId = params.get('tipoId');
    this.tipoPreferidoId = tipoId ? Number(tipoId) : null;
  }

  private aTexto(fecha: Date): string {
    const mes = `${fecha.getMonth() + 1}`.padStart(2, '0');
    const dia = `${fecha.getDate()}`.padStart(2, '0');
    return `${fecha.getFullYear()}-${mes}-${dia}`;
  }
}
