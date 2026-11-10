import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CabeceraComponent } from '../shared/cabecera/cabecera.component';
import { MenuMovilComponent } from '../shared/menu-movil/menu-movil.component';
import { PiePaginaComponent } from '../shared/pie-pagina/pie-pagina.component';
import { HeroInteriorComponent } from '../shared/hero-interior/hero-interior.component';
import { ConciergeService } from '../services/concierge.service';
import { ItinerarioResponse } from '../models/concierge.model';

@Component({
  selector: 'app-concierge-planner',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    CabeceraComponent,
    MenuMovilComponent,
    PiePaginaComponent,
    HeroInteriorComponent
  ],
  templateUrl: './concierge-planner.component.html',
  styleUrls: ['./concierge-planner.component.scss']
})
export class ConciergePlannerComponent {
  private conciergeService = inject(ConciergeService);

  menuAbierto = false;
  cargando = false;
  error: string | null = null;

  // Parámetros de formulario
  dias = 3;
  estilo = 'Luxury & Romance';
  acompanantes = 'Couple';
  preferencias = 'Fine dining, private spa sessions, Broadway theater, and panoramic sunset views.';

  itinerario: ItinerarioResponse | null = null;

  estilosDisponibles = [
    'Luxury & Romance',
    'Relaxation & Spa Sanctuary',
    'Epicurean & Wine Experience',
    'Executive Business & Focus',
    'Manhattan Art & Architecture'
  ];

  acompanantesDisponibles = [
    'Couple',
    'Solo Traveler',
    'Family with Children',
    'Executive Group'
  ];

  generarPlan(): void {
    this.cargando = true;
    this.error = null;

    this.conciergeService.generarItinerario({
      dias: this.dias,
      estilo: this.estilo,
      acompanantes: this.acompanantes,
      preferencias: this.preferencias
    }).subscribe({
      next: (res) => {
        this.itinerario = res;
        this.cargando = false;
      },
      error: () => {
        this.error = 'Could not generate itinerary. Please try again.';
        this.cargando = false;
      }
    });
  }

  imprimir(): void {
    window.print();
  }
}
