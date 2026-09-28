import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TipoHabitacionService } from '../../services/tipo-habitacion.service';
import { TipoHabitacion } from '../../models/tipo-habitacion.model';

@Component({
  selector: 'app-tipos-habitacion-formulario',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './tipos-habitacion-formulario.component.html'
})
export class TiposHabitacionFormularioComponent implements OnInit {

  // Si id es null el guardado crea; si tiene id, actualiza (igual que el campo oculto de Thymeleaf)
  id: number | null = null;
  nombre = '';
  descripcion = '';
  imagenUrl = '';
  error: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private tipoHabitacionService: TipoHabitacionService
  ) {}

  ngOnInit(): void {
    const parametroId = this.route.snapshot.paramMap.get('id');
    if (parametroId === null) {
      return;
    }

    const tipo = this.tipoHabitacionService.obtenerPorId(Number(parametroId));
    if (!tipo) {
      this.router.navigate(['/admin/tipos-habitacion']);
      return;
    }
    this.id = tipo.id;
    this.nombre = tipo.nombre;
    this.descripcion = tipo.descripcion;
    this.imagenUrl = tipo.imagenUrl;
  }

  guardar(): void {
    const datos = { nombre: this.nombre, descripcion: this.descripcion, imagenUrl: this.imagenUrl };
    try {
      if (this.id === null) {
        this.tipoHabitacionService.crear(datos);
      } else {
        const tipo: TipoHabitacion = { id: this.id, ...datos };
        this.tipoHabitacionService.actualizar(tipo);
      }
      this.router.navigate(['/admin/tipos-habitacion']);
    } catch (ex) {
      this.error = (ex as Error).message;
    }
  }
}
