import { Component, inject, OnInit, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ConciergeService } from '../../services/concierge.service';
import { MensajeChat } from '../../models/concierge.model';
import { ModalPagoStripeComponent } from '../modal-pago-stripe/modal-pago-stripe.component';

@Component({
  selector: 'app-chatbot-flotante',
  standalone: true,
  imports: [CommonModule, FormsModule, ModalPagoStripeComponent],
  templateUrl: './chatbot-flotante.component.html',
  styleUrls: ['./chatbot-flotante.component.scss']
})
export class ChatbotFlotanteComponent implements OnInit, AfterViewChecked {
  private conciergeService = inject(ConciergeService);
  private router = inject(Router);

  @ViewChild('cuerpoChat') private cuerpoChatRef!: ElementRef<HTMLDivElement>;

  abierto = false;
  textoInput = '';
  cargandoRespuesta = false;
  necesitaScroll = false;
  mostrarStripeModal = false;
  stripeMonto = 100;
  stripeReservaId = 1;

  mensajes: MensajeChat[] = [];

  ngOnInit(): void {
    this.mensajes.push({
      remitente: 'concierge',
      texto: 'Welcome to The Royale. I am your personal AI Concierge. How may I assist you with accommodations, dining reservations, or crafting your bespoke Manhattan itinerary today?',
      hora: this.obtenerHoraActual(),
      sugerencias: ['Suite Recommendations', 'Dining & Menus', 'Pay with Stripe 💳', 'Plan an Itinerary']
    });
  }

  ngAfterViewChecked(): void {
    if (this.necesitaScroll) {
      this.scrollAlFondo();
      this.necesitaScroll = false;
    }
  }

  toggleChat(): void {
    this.abierto = !this.abierto;
    if (this.abierto) {
      this.necesitaScroll = true;
    }
  }

  enviarTexto(): void {
    if (!this.textoInput.trim() || this.cargandoRespuesta) return;
    const msg = this.textoInput.trim();
    this.textoInput = '';
    this.procesarMensajeUsuario(msg);
  }

  seleccionarSugerencia(sugerencia: string): void {
    if (sugerencia.includes('Stripe')) {
      this.mostrarStripeModal = true;
      return;
    }
    this.procesarMensajeUsuario(sugerencia);
  }

  abrirStripe(): void {
    this.mostrarStripeModal = true;
  }

  navegarAccion(ruta?: string): void {
    if (ruta) {
      this.router.navigateByUrl(ruta);
      this.abierto = false;
    }
  }

  private procesarMensajeUsuario(texto: string): void {
    this.mensajes.push({
      remitente: 'usuario',
      texto,
      hora: this.obtenerHoraActual()
    });
    this.cargandoRespuesta = true;
    this.necesitaScroll = true;

    this.conciergeService.enviarMensaje(texto).subscribe({
      next: (resp) => {
        this.mensajes.push({
          remitente: 'concierge',
          texto: resp.respuesta,
          hora: this.obtenerHoraActual(),
          sugerencias: resp.sugerencias,
          enlaceAccion: resp.enlaceAccion,
          textoAccion: resp.textoAccion
        });
        this.cargandoRespuesta = false;
        this.necesitaScroll = true;
      },
      error: () => {
        this.mensajes.push({
          remitente: 'concierge',
          texto: 'I am momentarily attending to another distinguished guest. Please inquire about rooms, dining, or services, or click below to browse.',
          hora: this.obtenerHoraActual(),
          sugerencias: ['Rooms', 'Services', 'Book Now'],
          enlaceAccion: '/rooms',
          textoAccion: 'Browse Suites'
        });
        this.cargandoRespuesta = false;
        this.necesitaScroll = true;
      }
    });
  }

  private scrollAlFondo(): void {
    try {
      if (this.cuerpoChatRef) {
        this.cuerpoChatRef.nativeElement.scrollTop = this.cuerpoChatRef.nativeElement.scrollHeight;
      }
    } catch {}
  }

  private obtenerHoraActual(): string {
    const ahora = new Date();
    return ahora.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }
}
