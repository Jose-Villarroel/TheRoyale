import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CabeceraComponent } from '../shared/cabecera/cabecera.component';
import { MenuMovilComponent } from '../shared/menu-movil/menu-movil.component';
import { PiePaginaComponent } from '../shared/pie-pagina/pie-pagina.component';
import { HeroComponent } from './secciones/hero/hero.component';
import { BusquedaReserva, BuscadorReservaComponent } from './secciones/buscador-reserva/buscador-reserva.component';
import { SeccionHotelComponent } from './secciones/seccion-hotel/seccion-hotel.component';
import { SeccionCitaComponent } from './secciones/seccion-cita/seccion-cita.component';
import { CarruselHabitacionesComponent } from './secciones/carrusel-habitaciones/carrusel-habitaciones.component';
import { SeccionServiciosComponent } from './secciones/seccion-servicios/seccion-servicios.component';
import { SeccionExperienciasComponent } from './secciones/seccion-experiencias/seccion-experiencias.component';
import { SeccionBusinessComponent } from './secciones/seccion-business/seccion-business.component';
import { SeccionGaleriaComponent } from './secciones/seccion-galeria/seccion-galeria.component';
import { SeccionCtaComponent } from './secciones/seccion-cta/seccion-cta.component';
import { TipoHabitacionService } from '../services/tipo-habitacion.service';
import { ServicioService } from '../services/servicio.service';
import { TipoHabitacion } from '../models/tipo-habitacion.model';
import { Servicio } from '../models/servicio.model';

// Este componente solo arma la página: cada sección es su propio componente y se encarga
// de su propia lógica (scroll, parallax, carrusel). Aquí solo viven los datos compartidos.
@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [
    CabeceraComponent,
    MenuMovilComponent,
    PiePaginaComponent,
    HeroComponent,
    BuscadorReservaComponent,
    SeccionHotelComponent,
    SeccionCitaComponent,
    CarruselHabitacionesComponent,
    SeccionServiciosComponent,
    SeccionExperienciasComponent,
    SeccionBusinessComponent,
    SeccionGaleriaComponent,
    SeccionCtaComponent
  ],
  templateUrl: './landing.component.html'
})
export class LandingComponent implements OnInit {

  // ===== Datos traídos de la API REST del backend =====
  tiposHabitacion: TipoHabitacion[] = [];
  servicios: Servicio[] = [];

  // La cabecera pide abrirlo y el menú pide cerrarlo; el estado vive aquí por ser hermanos
  menuAbierto = false;

  // Si el backend no responde se deja la sección vacía, pero el error queda en consola:
  // sin rama de error un fallo de red se pierde como promesa rechazada y la página miente.
  error = '';

  constructor(
    private tipoHabitacionService: TipoHabitacionService,
    private servicioService: ServicioService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.tipoHabitacionService.obtenerTodos().subscribe({
      next: tipos => this.tiposHabitacion = tipos,
      error: err => this.manejarError(err)
    });
    this.servicioService.obtenerTodos().subscribe({
      next: servicios => this.servicios = servicios,
      error: err => this.manejarError(err)
    });
  }

  // ===== Formulario de reserva: por ahora solo redirige al login =====
  // El buscador no reserva: lleva los criterios a /book, que es donde se confirma
  manejarFormularioReserva(busqueda: BusquedaReserva): void {
    this.router.navigate(['/book'], {
      queryParams: {
        fechaInicio: busqueda.fechaInicio || null,
        fechaFin: busqueda.fechaFin || null,
        huespedes: busqueda.huespedes,
        tipoId: busqueda.tipoId
      }
    });
  }

  private manejarError(err: unknown): void {
    this.error = 'No se pudieron cargar los datos del hotel. Intenta de nuevo más tarde.';
    console.error('Error cargando los datos de la landing', err);
  }
}
