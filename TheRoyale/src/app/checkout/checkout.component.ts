import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { CabeceraComponent } from '../shared/cabecera/cabecera.component';
import { MenuMovilComponent } from '../shared/menu-movil/menu-movil.component';
import { PiePaginaComponent } from '../shared/pie-pagina/pie-pagina.component';
import { HeroInteriorComponent } from '../shared/hero-interior/hero-interior.component';
import { ModalPagoStripeComponent } from '../shared/modal-pago-stripe/modal-pago-stripe.component';
import { StripeService } from '../services/stripe.service';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    CabeceraComponent,
    MenuMovilComponent,
    PiePaginaComponent,
    HeroInteriorComponent,
    ModalPagoStripeComponent
  ],
  templateUrl: './checkout.component.html',
  styleUrls: ['./checkout.component.scss']
})
export class CheckoutComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private stripeService = inject(StripeService);

  menuAbierto = false;
  mostrarModalStripe = false;
  pagoRealizado = false;

  reservaId = 1;
  monto = 360.00;
  concepto = 'Room Guarantee & Luxury Lodging Deposit';

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      if (params['reservaId']) {
        this.reservaId = Number(params['reservaId']) || 1;
      }
      if (params['monto']) {
        this.monto = Number(params['monto']) || 360.00;
      }
    });
  }

  abrirStripe(): void {
    this.mostrarModalStripe = true;
  }

  onPagoExitoso(): void {
    this.pagoRealizado = true;
  }
}
