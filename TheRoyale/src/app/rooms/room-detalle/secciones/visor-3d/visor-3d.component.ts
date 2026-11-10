import { Component, Input, OnInit, OnChanges, SimpleChanges, OnDestroy, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface Vista3d {
  id: string;
  nombre: string;
  url: string;
  icono: string;
}

export interface PuntoInteres {
  id: string;
  vista: string;
  x: number;
  y: number;
  titulo: string;
  desc: string;
}

@Component({
  selector: 'app-visor-3d',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './visor-3d.component.html',
  styleUrls: ['./visor-3d.component.scss']
})
export class Visor3dComponent implements OnInit, OnChanges, OnDestroy {
  @Input() tipoNombre = 'Luxury Suite';
  @Input() imagenPanorama = '/images/suite-1.webp';

  vistas: Vista3d[] = [
    { id: 'suite', nombre: 'Suite Principal', url: '/images/suite-1.webp', icono: '🛏️' },
    { id: 'skyline', nombre: 'Terraza Skyline', url: '/images/luxury 1.jpg', icono: '🌆' },
    { id: 'spa', nombre: 'Baño de Mármol & Spa', url: '/images/luxury 2.jpg', icono: '🛁' }
  ];

  vistaActivaId = 'suite';
  imagenActual = '/images/suite-1.webp';

  anguloX = 45;
  anguloY = 0;
  arrastrando = false;
  startX = 0;
  startY = 0;
  autoRotacion = true;
  pantallaCompleta = false;
  puntoActivo: string | null = null;
  zoomNivel = 1.0;
  headingCompass = 45;

  puntosInteres: PuntoInteres[] = [
    { id: 'cama', vista: 'suite', x: 30, y: 55, titulo: 'Bespoke King Bed', desc: 'Custom 600-thread count Egyptian cotton linens and down pillows.' },
    { id: 'vista', vista: 'suite', x: 68, y: 42, titulo: 'Manhattan Skyline View', desc: 'Floor-to-ceiling soundproof acoustic panoramic windows with electric blackout shades.' },
    { id: 'bano', vista: 'suite', x: 86, y: 65, titulo: 'Carrara Marble Bath', desc: 'Deep soaking tub and rainfall shower with bespoke Hermès amenities.' },
    { id: 'audio', vista: 'suite', x: 15, y: 60, titulo: 'Bang & Olufsen Spatial Audio', desc: 'Acoustic smart audio integrated throughout the bedroom and lounge.' },
    { id: 'skydeck', vista: 'skyline', x: 50, y: 50, titulo: 'Private Heated Skydeck', desc: 'Breathtaking 360° unobstructed views overlooking Central Park and Midtown.' },
    { id: 'jacuzzi', vista: 'spa', x: 48, y: 58, titulo: 'Thermal Hydro Jacuzzi', desc: 'Multi-jet hydrotherapy bath with volcanic mineral salts.' }
  ];

  private animFrameId: number | null = null;

  ngOnInit(): void {
    this.sincronizarImagen();
    this.iniciarRenderLoop();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['imagenPanorama'] && this.imagenPanorama) {
      this.sincronizarImagen();
    }
  }

  ngOnDestroy(): void {
    if (this.animFrameId) {
      cancelAnimationFrame(this.animFrameId);
    }
  }

  get estiloFondo(): string {
    const urlSegura = (this.imagenActual || '/images/suite-1.webp').replace(/ /g, '%20');
    return `url('${urlSegura}')`;
  }

  private sincronizarImagen(): void {
    const url = this.imagenPanorama && this.imagenPanorama.trim().length > 0
      ? this.imagenPanorama
      : '/images/suite-1.webp';
    this.vistas[0].url = url;
    if (this.vistaActivaId === 'suite') {
      this.imagenActual = url;
    }
  }

  cambiarVista(id: string): void {
    this.vistaActivaId = id;
    const vista = this.vistas.find(v => v.id === id);
    if (vista) {
      this.imagenActual = vista.url;
    }
    this.puntoActivo = null;
    this.anguloX = 0;
    this.anguloY = 0;
  }

  get puntosFiltrados(): PuntoInteres[] {
    return this.puntosInteres.filter(p => p.vista === this.vistaActivaId);
  }

  private iniciarRenderLoop(): void {
    const loop = () => {
      if (this.autoRotacion && !this.arrastrando) {
        this.anguloX = (this.anguloX + 0.12) % 360;
      }
      this.headingCompass = Math.round(((this.anguloX % 360) + 360) % 360);
      this.animFrameId = requestAnimationFrame(loop);
    };
    this.animFrameId = requestAnimationFrame(loop);
  }

  onMouseDown(e: MouseEvent): void {
    this.arrastrando = true;
    this.startX = e.clientX;
    this.startY = e.clientY;
    this.autoRotacion = false;
  }

  onMouseMove(e: MouseEvent): void {
    if (!this.arrastrando) return;
    const dx = e.clientX - this.startX;
    const dy = e.clientY - this.startY;
    this.anguloX = (this.anguloX - dx * 0.2) % 360;
    this.anguloY = Math.max(-25, Math.min(25, this.anguloY + dy * 0.15));
    this.startX = e.clientX;
    this.startY = e.clientY;
  }

  @HostListener('window:mouseup')
  onMouseUp(): void {
    this.arrastrando = false;
  }

  onTouchStart(e: TouchEvent): void {
    if (e.touches.length === 1) {
      this.arrastrando = true;
      this.startX = e.touches[0].clientX;
      this.startY = e.touches[0].clientY;
      this.autoRotacion = false;
    }
  }

  onTouchMove(e: TouchEvent): void {
    if (!this.arrastrando || e.touches.length !== 1) return;
    const dx = e.touches[0].clientX - this.startX;
    const dy = e.touches[0].clientY - this.startY;
    this.anguloX = (this.anguloX - dx * 0.25) % 360;
    this.anguloY = Math.max(-25, Math.min(25, this.anguloY + dy * 0.2));
    this.startX = e.touches[0].clientX;
    this.startY = e.touches[0].clientY;
  }

  @HostListener('window:touchend')
  onTouchEnd(): void {
    this.arrastrando = false;
  }

  onWheelZoom(e: WheelEvent): void {
    e.preventDefault();
    if (e.deltaY < 0) {
      this.zoomIn();
    } else {
      this.zoomOut();
    }
  }

  toggleAutoRotacion(): void {
    this.autoRotacion = !this.autoRotacion;
  }

  zoomIn(): void {
    this.zoomNivel = Math.min(1.4, Number((this.zoomNivel + 0.08).toFixed(2)));
  }

  zoomOut(): void {
    this.zoomNivel = Math.max(0.85, Number((this.zoomNivel - 0.08).toFixed(2)));
  }

  reiniciarVista(): void {
    this.anguloX = 0;
    this.anguloY = 0;
    this.zoomNivel = 1.0;
    this.autoRotacion = true;
  }

  togglePantallaCompleta(): void {
    this.pantallaCompleta = !this.pantallaCompleta;
  }

  seleccionarPunto(id: string): void {
    this.puntoActivo = this.puntoActivo === id ? null : id;
  }
}
