import { AfterViewInit, Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TipoHabitacionService } from '../services/tipo-habitacion.service';
import { ServicioService } from '../services/servicio.service';
import { TipoHabitacion } from '../models/tipo-habitacion.model';
import { Servicio } from '../models/servicio.model';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './landing.component.html'
})
export class LandingComponent implements OnInit, AfterViewInit, OnDestroy {

  // ===== Datos traídos de los servicios (base de datos quemada) =====
  tiposHabitacion: TipoHabitacion[] = [];
  servicios: Servicio[] = [];

  // ===== Estado del carrusel de habitaciones =====
  habitacionActual = 0;

  private manejadorScroll = () => this.actualizarCabeceraEnScroll();

  constructor(
    private tipoHabitacionService: TipoHabitacionService,
    private servicioService: ServicioService
  ) {}

  ngOnInit(): void {
    this.tiposHabitacion = this.tipoHabitacionService.obtenerTodos();
    this.servicios = this.servicioService.obtenerTodos();
  }

  ngAfterViewInit(): void {
    window.addEventListener('scroll', this.manejadorScroll, { passive: true });
    this.actualizarCabeceraEnScroll();
    this.activarAnimacionesAparicion();
    this.actualizarCarrusel();
  }

  ngOnDestroy(): void {
    window.removeEventListener('scroll', this.manejadorScroll);
  }

  // ===== Header sólido al hacer scroll =====
  private actualizarCabeceraEnScroll(): void {
    const envoltorio = document.getElementById('envoltorioCabecera');
    const heroMedia = document.getElementById('heroMedia');
    const y = window.scrollY || 0;

    if (envoltorio) {
      envoltorio.classList.toggle('cabecera-solida', y > 90);
    }

    if (heroMedia) {
      const desplazamiento = Math.min(y * 0.13, 120);
      heroMedia.style.transform = `translateY(${desplazamiento}px)`;
    }
  }

  // ===== Menú móvil =====
  abrirMenuMovil(): void {
    document.getElementById('menuMovil')?.classList.add('abierto');
  }

  cerrarMenuMovil(): void {
    document.getElementById('menuMovil')?.classList.remove('abierto');
  }

  // ===== Animación de aparición al hacer scroll =====
  private activarAnimacionesAparicion(): void {
    const elementos = document.querySelectorAll('.aparecer');
    if (elementos.length === 0) {
      return;
    }

    const observador = new IntersectionObserver(
      entradas => {
        entradas.forEach(entrada => {
          if (entrada.isIntersecting) {
            entrada.target.classList.add('visible');
            observador.unobserve(entrada.target);
          }
        });
      },
      { rootMargin: '0px 0px -8% 0px', threshold: 0.08 }
    );

    elementos.forEach(elemento => observador.observe(elemento));
  }

  // ===== Carrusel de habitaciones =====
  private actualizarCarrusel(): void {
    const track = document.getElementById('carruselTrack');
    const progreso = document.getElementById('carruselProgreso');
    const contador = document.getElementById('carruselContador');
    const total = this.tiposHabitacion.length;

    if (!track || !progreso || !contador) {
      return;
    }

    if (total === 0) {
      contador.textContent = '00 / 00';
      progreso.style.width = '0';
      return;
    }

    track.style.transform = `translateX(-${this.habitacionActual * 100}%)`;
    progreso.style.width = `${100 / total}%`;
    progreso.style.transform = `translateX(${this.habitacionActual * 100}%)`;

    const numeroActual = String(this.habitacionActual + 1).padStart(2, '0');
    const numeroTotal = String(total).padStart(2, '0');
    contador.textContent = `${numeroActual} / ${numeroTotal}`;
  }

  irHabitacionAnterior(): void {
    const total = this.tiposHabitacion.length;
    if (total === 0) return;
    this.habitacionActual = (this.habitacionActual - 1 + total) % total;
    this.actualizarCarrusel();
  }

  irHabitacionSiguiente(): void {
    const total = this.tiposHabitacion.length;
    if (total === 0) return;
    this.habitacionActual = (this.habitacionActual + 1) % total;
    this.actualizarCarrusel();
  }

  // ===== Formulario de reserva: por ahora solo redirige (login no existe todavía en Angular) =====
  manejarFormularioReserva(evento: Event): void {
    evento.preventDefault();
    window.location.href = '/login';
  }

  manejarFormularioNewsletter(evento: Event): void {
    evento.preventDefault();
    alert('Thank you for subscribing to The Royale newsletter.');
    (evento.target as HTMLFormElement).reset();
  }
}