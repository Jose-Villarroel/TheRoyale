import { Component, input, output } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';

// Formulario para registrar un pago contra el saldo (AC25). Al saldar la cuenta, el backend
// marca los cargos como pagados. La página crea el FormGroup y hace las peticiones;
// aquí solo se emiten (registrar) y (pagarSaldo), que copia el saldo completo al monto.
@Component({
  selector: 'app-form-pago',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './form-pago.component.html'
})
export class FormPagoComponent {
  formulario = input.required<FormGroup>();
  metodosPago = input.required<readonly string[]>();
  saldo = input.required<number>();
  registrar = output<void>();
  pagarSaldo = output<void>();
}
