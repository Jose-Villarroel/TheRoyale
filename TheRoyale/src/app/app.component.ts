import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ChatbotFlotanteComponent } from './shared/chatbot-flotante/chatbot-flotante.component';

// Componente raíz: aloja el <router-outlet> y el Chatbot Concierge Flotante global.
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, ChatbotFlotanteComponent],
  template: `
    <router-outlet></router-outlet>
    <app-chatbot-flotante></app-chatbot-flotante>
  `
})
export class AppComponent {}
