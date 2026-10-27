import { Component, input, output } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { CampoErrorComponent } from '../../../../shared/campo-error/campo-error.component';

// Formulario "Edit Profile": nombre, apellido, email, teléfono y contraseña nueva.
// La página crea y valida el FormGroup y hace la petición al recibir (guardar);
// (cancelar) le pide que vuelva a cargar los datos guardados.
@Component({
  selector: 'app-form-perfil',
  standalone: true,
  imports: [ReactiveFormsModule, CampoErrorComponent],
  templateUrl: './form-perfil.component.html'
})
export class FormPerfilComponent {
  formulario = input.required<FormGroup>();
  guardar = output<void>();
  cancelar = output<void>();
}
