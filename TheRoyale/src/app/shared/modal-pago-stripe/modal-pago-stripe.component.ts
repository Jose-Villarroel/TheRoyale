import { Component, Input, Output, EventEmitter, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { StripeService } from '../../services/stripe.service';

@Component({
  selector: 'app-modal-pago-stripe',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './modal-pago-stripe.component.html',
  styleUrls: ['./modal-pago-stripe.component.scss']
})
export class ModalPagoStripeComponent {
  @Input() visible = false;
  @Input() reservaId!: number;
  @Input() monto = 100;
  @Input() descripcion = 'The Royale Room Reservation';

  @Output() cerrar = new EventEmitter<void>();
  @Output() pagoExitoso = new EventEmitter<void>();

  private stripeService = inject(StripeService);

  titular = 'Valued Guest';
  numeroTarjeta = '•••• •••• •••• 4242';
  expiracion = '12/28';
  cvc = '888';
  procesando = false;
  completado = false;
  error: string | null = null;

  procesarPago(): void {
    this.procesando = true;
    this.error = null;

    // 1. Crear Intent en backend
    this.stripeService.crearPaymentIntent({
      reservaId: this.reservaId,
      monto: this.monto,
      moneda: 'usd'
    }).subscribe({
      next: (intent) => {
        // 2. Confirmar pago de forma segura
        this.stripeService.confirmarPago({
          reservaId: this.reservaId,
          paymentIntentId: intent.paymentIntentId,
          monto: this.monto
        }).subscribe({
          next: () => {
            this.procesando = false;
            this.completado = true;
            setTimeout(() => {
              this.pagoExitoso.emit();
              this.cerrarModal();
            }, 1800);
          },
          error: (err) => {
            this.error = err.error?.message || 'Payment processing error. Please try again.';
            this.procesando = false;
          }
        });
      },
      error: (err) => {
        this.error = err.error?.message || 'Could not initialize Stripe intent.';
        this.procesando = false;
      }
    });
  }

  cerrarModal(): void {
    this.completado = false;
    this.procesando = false;
    this.error = null;
    this.cerrar.emit();
  }
}
