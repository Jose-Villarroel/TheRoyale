import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { TipoHabitacionService } from '../../services/tipo-habitacion.service';
import { TipoHabitacion } from '../../models/tipo-habitacion.model';

@Component({
  selector: 'app-tipos-habitacion-lista',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './tipos-habitacion-lista.component.html'
})
export class TiposHabitacionListaComponent implements OnInit {

  tipos: TipoHabitacion[] = [];
  error: string | null = null;

  constructor(private tipoHabitacionService: TipoHabitacionService) {}

  ngOnInit(): void {
    this.tipos = this.tipoHabitacionService.obtenerTodos();
  }

  eliminar(tipo: TipoHabitacion): void {
    if (!confirm('Delete this room type?')) {
      return;
    }
    try {
      this.tipoHabitacionService.eliminar(tipo.id);
      this.error = null;
    } catch (ex) {
      this.error = (ex as Error).message;
    }
  }
}
