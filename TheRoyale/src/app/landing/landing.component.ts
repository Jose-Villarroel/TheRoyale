import { Component, OnInit } from '@angular/core';
import { CabeceraComponent } from '../shared/cabecera/cabecera.component';
import { MenuMovilComponent } from '../shared/menu-movil/menu-movil.component';
import { PiePaginaComponent } from '../shared/pie-pagina/pie-pagina.component';
import { HeroComponent } from './secciones/hero/hero.component';
import { BuscadorReservaComponent } from './secciones/buscador-reserva/buscador-reserva.component';
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

  // ===== Datos traídos de los servicios (base de datos quemada) =====
  tiposHabitacion: TipoHabitacion[] = [];
  servicios: Servicio[] = [];

  // La cabecera pide abrirlo y el menú pide cerrarlo; el estado vive aquí por ser hermanos
  menuAbierto = false;

  constructor(
    private tipoHabitacionService: TipoHabitacionService,
    private servicioService: ServicioService
  ) {}

  ngOnInit(): void {
    this.tiposHabitacion = this.tipoHabitacionService.obtenerTodos();
    this.servicios = this.servicioService.obtenerTodos();
  }

  // ===== Formulario de reserva: por ahora solo redirige (login no existe todavía en Angular) =====
  manejarFormularioReserva(evento: Event): void {
    evento.preventDefault();
    window.location.href = '/login';
  }
}
