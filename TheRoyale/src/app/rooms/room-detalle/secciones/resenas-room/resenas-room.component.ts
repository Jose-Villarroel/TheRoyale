import { Component, Input, OnChanges, SimpleChanges, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ResenaService } from '../../../../services/resena.service';
import { AuthService } from '../../../../services/auth.service';
import { Resena, ResumenResenas } from '../../../../models/resena.model';

@Component({
  selector: 'app-resenas-room',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './resenas-room.component.html',
  styleUrls: ['./resenas-room.component.scss']
})
export class ResenasRoomComponent implements OnChanges {
  @Input() tipoId!: number;
  @Input() tipoNombre = '';

  private resenaService = inject(ResenaService);
  private authService = inject(AuthService);

  resumen: ResumenResenas | null = null;
  cargando = true;

  // Formulario nueva reseña
  mostrarFormulario = false;
  nuevaEstrella = 5;
  nuevoTitulo = '';
  nuevoComentario = '';
  guardando = false;
  mensajeExito = '';
  errorForm = '';

  get usuario() {
    return this.authService.usuario;
  }

  get esCliente() {
    return this.authService.esCliente;
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['tipoId'] && this.tipoId) {
      this.cargarResenas();
    }
  }

  cargarResenas(): void {
    this.cargando = true;
    this.resenaService.obtenerResumenPorTipo(this.tipoId).subscribe({
      next: (res) => {
        this.resumen = res;
        this.cargando = false;
      },
      error: () => {
        this.cargando = false;
      }
    });
  }

  setEstrellas(n: number): void {
    this.nuevaEstrella = n;
  }

  enviarResena(): void {
    if (!this.usuario || !this.esCliente) {
      this.errorForm = 'You must be signed in as a guest to leave a review.';
      return;
    }
    if (!this.nuevoComentario.trim()) {
      this.errorForm = 'Please write a review comment.';
      return;
    }

    this.guardando = true;
    this.errorForm = '';

    this.resenaService.crearResena({
      clienteId: this.usuario.id,
      tipoHabitacionId: this.tipoId,
      estrellas: this.nuevaEstrella,
      titulo: this.nuevoTitulo.trim() || 'Verified Guest Experience',
      comentario: this.nuevoComentario.trim()
    }).subscribe({
      next: () => {
        this.guardando = false;
        this.mensajeExito = 'Thank you! Your verified review has been published.';
        this.mostrarFormulario = false;
        this.nuevoTitulo = '';
        this.nuevoComentario = '';
        this.cargarResenas();
      },
      error: (err) => {
        this.errorForm = err.error?.message || 'Error submitting review. Please try again.';
        this.guardando = false;
      }
    });
  }

  arrayEstrellas(n: number): number[] {
    return Array(Math.round(n)).fill(0);
  }

  arrayVacias(n: number): number[] {
    return Array(Math.max(0, 5 - Math.round(n))).fill(0);
  }
}
