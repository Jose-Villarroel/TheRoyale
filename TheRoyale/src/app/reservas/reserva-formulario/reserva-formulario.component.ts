import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { of, switchMap } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { CabeceraComponent } from '../../shared/cabecera/cabecera.component';
import { MenuMovilComponent } from '../../shared/menu-movil/menu-movil.component';
import { PiePaginaComponent } from '../../shared/pie-pagina/pie-pagina.component';
import { HeroInteriorComponent } from '../../shared/hero-interior/hero-interior.component';
import { AlertaErrorComponent } from '../../shared/alerta-error/alerta-error.component';
import { PasoFechasComponent } from './secciones/paso-fechas/paso-fechas.component';
import { PasoHabitacionComponent } from './secciones/paso-habitacion/paso-habitacion.component';
import { ResumenReservaComponent } from './secciones/resumen-reserva/resumen-reserva.component';
import { ReservaConfirmadaComponent } from './secciones/reserva-confirmada/reserva-confirmada.component';
import { DisponibilidadService } from '../../services/disponibilidad.service';
import { ReservaService } from '../../services/reserva.service';
import { AuthService } from '../../services/auth.service';
import { DisponibilidadTipo, Reserva } from '../../models/reserva.model';
import { diaSiguiente, hoyLocal, noches } from '../../utils/fechas';

// Reserva en tres pasos: fechas -> tipo de habitación -> confirmación.
// La habitación concreta no se elige aquí: la asigna el backend al crear la reserva.
// La página guarda el formulario y el estado; cada paso es un componente de secciones/.
@Component({
  selector: 'app-reserva-formulario',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    CabeceraComponent,
    MenuMovilComponent,
    PiePaginaComponent,
    HeroInteriorComponent,
    AlertaErrorComponent,
    PasoFechasComponent,
    PasoHabitacionComponent,
    ResumenReservaComponent,
    ReservaConfirmadaComponent
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
  readonly hoy = hoyLocal();

  // Reserva recien creada: mientras exista, la pagina muestra la confirmacion en vez del formulario
  confirmada: Reserva | null = null;
  disponibilidad: DisponibilidadTipo[] = [];
  seleccionado: DisponibilidadTipo | null = null;
  // Tipo que venia en la url de la landing: solo una pista hasta que llegue la disponibilidad real
  private tipoPreferidoId: number | null = null;
  // Con ?reservaId= la pagina edita esa reserva en vez de crear una nueva (AC18)
  reservaIdEnEdicion: number | null = null;
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
    return inicio ? diaSiguiente(inicio) : this.hoy;
  }

  get noches(): number {
    if (!this.rangoValido) {
      return 0;
    }
    const { fechaInicio, fechaFin } = this.reservaForm.value;
    return noches(fechaInicio, fechaFin);
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
        tipoId: this.seleccionado?.tipoHabitacionId ?? null,
        reservaId: this.reservaIdEnEdicion
      }
    }).toString();
  }

  elegirTipo(tipo: DisponibilidadTipo): void {
    this.seleccionado = tipo;
  }

  confirmar(): void {
    // Solo un huésped reserva: el operador también tiene sesión, pero su id no es un clienteId
    const cliente = this.authService.usuario;
    if (!this.puedeConfirmar || !cliente || !this.authService.esCliente) {
      return;
    }

    const solicitud = {
      clienteId: cliente.id,
      tipoHabitacionId: this.seleccionado!.tipoHabitacionId,
      fechaInicio: this.reservaForm.value.fechaInicio,
      fechaFin: this.reservaForm.value.fechaFin,
      cantidadPersonas: Number(this.reservaForm.value.cantidadPersonas)
    };

    this.enviando = true;
    const peticion = this.reservaIdEnEdicion
      ? this.reservaService.modificar(this.reservaIdEnEdicion, solicitud)
      : this.reservaService.crear(solicitud);

    peticion.subscribe({
      next: (reserva) => {
        this.enviando = false;
        // No se navega a ningun listado: la confirmacion se muestra aqui mismo
        this.confirmada = reserva;
        window.scrollTo({ top: 0, behavior: 'smooth' });
      },
      error: (err) => {
        this.enviando = false;
        // 409 cuando otra reserva ocupó la última habitación mientras se llenaba el formulario
        this.error = err.error?.message || err.message
          || (this.reservaIdEnEdicion ? 'Error modifying reservation' : 'Error creating reservation');
        this.refrescarDisponibilidad();
      }
    });
  }

  reservarOtra(): void {
    this.confirmada = null;
    this.seleccionado = null;
    this.reservaIdEnEdicion = null;
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

    const reservaId = params.get('reservaId');
    this.reservaIdEnEdicion = reservaId ? Number(reservaId) : null;
  }
}
