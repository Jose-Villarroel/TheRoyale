import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ServicioService } from '../../services/servicio.service';
import { Servicio } from '../../models/servicio.model';

@Component({
  selector: 'app-servicios-lista',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './servicios-lista.component.html'
})
export class ServiciosListaComponent implements OnInit {

  servicios: Servicio[] = [];
  error: string | null = null;

  constructor(private servicioService: ServicioService) {}

  ngOnInit(): void {
    this.servicios = this.servicioService.obtenerTodos();
  }

  eliminar(servicio: Servicio): void {
    if (!confirm('Delete this service?')) {
      return;
    }
    try {
      this.servicioService.eliminar(servicio.id);
      this.error = null;
    } catch (ex) {
      this.error = (ex as Error).message;
    }
  }
}