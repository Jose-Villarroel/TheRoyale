import { Component, effect, inject, input, output } from '@angular/core';
import { DatePipe } from '@angular/common';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { EstrellasComponent } from '../../../../shared/estrellas/estrellas.component';
import { CampoErrorComponent } from '../../../../shared/campo-error/campo-error.component';
import { AlertaErrorComponent } from '../../../../shared/alerta-error/alerta-error.component';
import { Reserva } from '../../../../models/reserva.model';
import {
  DatosResena,
  Resena,
  RESENA_COMENTARIO_MAX,
  RESENA_COMENTARIO_MIN,
  RESENA_TITULO_MAX
} from '../../../../models/resena.model';

// Longitud medida tras trim (RN-5): un comentario de puros espacios no cuenta como escrito
function longitudRecortada(min: number, max: number): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const texto = String(control.value ?? '').trim();
    if (texto.length === 0) {
      return { required: true };
    }
    if (texto.length < min) {
      return { minlength: { requiredLength: min, actualLength: texto.length } };
    }
    if (texto.length > max) {
      return { maxlength: { requiredLength: max, actualLength: texto.length } };
    }
    return null;
  };
}

// Formulario para escribir o editar la reseña de una estadía FINALIZADA.
// Valida en el cliente RN-4 (1 a 5 estrellas) y RN-5 (comentario de 10 a 1000, título hasta 100);
// los errores del backend llegan por [error]. No llama al API: avisa a la página con
// (enviar), (eliminar) y (cancelar), y la página hace la petición.
@Component({
  selector: 'app-form-resena',
  standalone: true,
  imports: [DatePipe, ReactiveFormsModule, EstrellasComponent, CampoErrorComponent, AlertaErrorComponent],
  templateUrl: './form-resena.component.html'
})
export class FormResenaComponent {
  private fb = inject(FormBuilder);

  reserva = input.required<Reserva>();
  // null = reseña nueva; con valor, el formulario arranca con sus datos y ofrece "Delete review"
  resena = input<Resena | null>(null);
  error = input<string | null>(null);
  enviando = input<boolean>(false);

  enviar = output<DatosResena>();
  eliminar = output<void>();
  cancelar = output<void>();

  readonly maxComentario = RESENA_COMENTARIO_MAX;
  readonly mensajesCalificacion = { required: 'Please choose a rating from 1 to 5 stars.', min: 'The rating must be between 1 and 5.', max: 'The rating must be between 1 and 5.' };
  readonly mensajesTitulo = { maxlength: `The title can have at most ${RESENA_TITULO_MAX} characters.` };
  readonly mensajesComentario = {
    required: 'Please tell us about your stay.',
    minlength: `The comment must have at least ${RESENA_COMENTARIO_MIN} characters.`,
    maxlength: `The comment can have at most ${RESENA_COMENTARIO_MAX} characters.`
  };

  readonly formulario = this.fb.group({
    calificacion: this.fb.control<number | null>(null, [Validators.required, Validators.min(1), Validators.max(5)]),
    titulo: this.fb.control<string>('', [Validators.maxLength(RESENA_TITULO_MAX)]),
    comentario: this.fb.control<string>('', [longitudRecortada(RESENA_COMENTARIO_MIN, RESENA_COMENTARIO_MAX)])
  });

  constructor() {
    // Al abrir el formulario para otra reserva (o al editar) se vuelve a cargar con sus datos
    effect(() => {
      const resena = this.resena();
      this.reserva();
      this.formulario.reset({
        calificacion: resena?.calificacion ?? null,
        titulo: resena?.titulo ?? '',
        comentario: resena?.comentario ?? ''
      });
    });
  }

  get calificacion() { return this.formulario.controls.calificacion; }
  get titulo() { return this.formulario.controls.titulo; }
  get comentario() { return this.formulario.controls.comentario; }

  // Contador "n/1000" sobre el texto recortado, que es lo que valida el backend
  get largoComentario(): number {
    return (this.comentario.value ?? '').trim().length;
  }

  elegirCalificacion(valor: number | null): void {
    this.calificacion.setValue(valor);
    this.calificacion.markAsTouched();
  }

  enviarFormulario(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    const { calificacion, titulo, comentario } = this.formulario.getRawValue();
    const tituloLimpio = (titulo ?? '').trim();
    this.enviar.emit({
      calificacion: calificacion as number,
      titulo: tituloLimpio.length > 0 ? tituloLimpio : null,
      comentario: (comentario ?? '').trim()
    });
  }
}
