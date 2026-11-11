import { Component, input, output } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Servicio } from '../../../../models/servicio.model';

// Formulario para cargar un servicio a la cuenta (AC23). El FormGroup lo crea y valida
// la página; este componente solo lo pinta y avisa con (agregar) al enviarlo.
@Component({
  selector: 'app-form-consumo',
  standalone: true,
  imports: [CurrencyPipe, ReactiveFormsModule],
  templateUrl: './form-consumo.component.html'
})
export class FormConsumoComponent {
  formulario = input.required<FormGroup>();
  servicios = input.required<Servicio[]>();
  agregar = output<void>();
}
