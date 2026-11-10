import { Component, Input, ElementRef, ViewChild, AfterViewInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-visor-3d',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './visor-3d.component.html',
  styleUrls: ['./visor-3d.component.scss']
})
export class Visor3dComponent implements AfterViewInit, OnDestroy {
  @Input() tipoNombre = 'Suite';
  @Input() imagenPanorama = '/images/interior 1.jpg';

  @ViewChild('panoramaCanvas', { static: false }) canvasRef!: ElementRef<HTMLCanvasElement>;

  anguloX = 0;
  anguloY = 0;
  arrastrando = false;
  startX = 0;
  startY = 0;
  autoRotacion = true;
  pantallaCompleta = false;
  puntoActivo: string | null = null;

  puntosInteres = [
    { id: 'cama', x: 28, y: 55, titulo: 'Bespoke King Bed', desc: 'Custom 600-thread count Egyptian cotton linens.' },
    { id: 'vista', x: 65, y: 40, titulo: 'Manhattan Skyline View', desc: 'Floor-to-ceiling soundproof acoustic panoramic windows.' },
    { id: 'bano', x: 82, y: 62, titulo: 'Carrara Marble Bath', desc: 'Deep soaking tub and rainfall shower with Hermès amenities.' }
  ];

  private animFrameId: number | null = null;

  ngAfterViewInit(): void {
    this.iniciarRenderLoop();
  }

  ngOnDestroy(): void {
    if (this.animFrameId) {
      cancelAnimationFrame(this.animFrameId);
    }
  }

  private iniciarRenderLoop(): void {
    const loop = () => {
      if (this.autoRotacion && !this.arrastrando) {
        this.anguloX = (this.anguloX + 0.15) % 360;
      }
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
    this.anguloX = (this.anguloX - dx * 0.4) % 360;
    this.anguloY = Math.max(-25, Math.min(25, this.anguloY + dy * 0.2));
    this.startX = e.clientX;
    this.startY = e.clientY;
  }

  onMouseUp(): void {
    this.arrastrando = false;
  }

  toggleAutoRotacion(): void {
    this.autoRotacion = !this.autoRotacion;
  }

  togglePantallaCompleta(): void {
    this.pantallaCompleta = !this.pantallaCompleta;
  }

  seleccionarPunto(id: string): void {
    this.puntoActivo = this.puntoActivo === id ? null : id;
  }
}
