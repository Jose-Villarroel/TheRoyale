import { Injectable } from '@angular/core';
import { TipoHabitacion } from '../models/tipo-habitacion.model';
import { HabitacionService } from './habitacion.service';

@Injectable({ providedIn: 'root' })
export class TipoHabitacionService {

  constructor(private habitacionService: HabitacionService) {}

  // ===== "Base de datos" quemada, con los mismos 5 tipos que ya tienes en el backend =====
  // Vive en memoria: los cambios se ven mientras no se recargue la página (F5 vuelve a estos datos)
  private tiposHabitacion: TipoHabitacion[] = [
    { id: 1, nombre: 'Normal', descripcion: 'Elegant essentials for a refined stay. 1-2 guests, double bed, private bathroom, premium Wi-Fi.', imagenUrl: '/images/suite-3.webp' },
    { id: 2, nombre: 'Executive', descripcion: 'Designed for those who work while they travel. 1-2 guests, executive workspace, king bed, premium Wi-Fi.', imagenUrl: '/images/suite-1.webp' },
    { id: 3, nombre: 'VIP', descripcion: 'A private space to relax and unwind. Up to 3 guests, separate living area, premium amenities, city views.', imagenUrl: '/images/suite-2.webp' },
    { id: 4, nombre: 'Luxury', descripcion: 'The ultimate expression of The Royale. Up to 4 guests, full suite, jacuzzi, privileged city view.', imagenUrl: '/images/suite-4.webp' },
    { id: 5, nombre: 'Presidential Suite', descripcion: 'Unmatched exclusivity. Up to 6 guests, private terrace, butler service, panoramic city view, private dining.', imagenUrl: '/images/luxury 1.jpg' }
  ];

  obtenerTodos(): TipoHabitacion[] {
    return this.tiposHabitacion;
  }

  obtenerPorId(id: number): TipoHabitacion | undefined {
    return this.tiposHabitacion.find(tipo => tipo.id === id);
  }

  // ===== CRUD: mismas reglas que TipoHabitacionService del backend =====
  crear(tipo: Omit<TipoHabitacion, 'id'>): TipoHabitacion {
    const nombre = this.validarNombre(tipo.nombre, null);
    const siguienteId = Math.max(0, ...this.tiposHabitacion.map(t => t.id)) + 1;
    const nuevo: TipoHabitacion = { ...tipo, id: siguienteId, nombre };
    this.tiposHabitacion.push(nuevo);
    return nuevo;
  }

  actualizar(tipo: TipoHabitacion): TipoHabitacion {
    const indice = this.tiposHabitacion.findIndex(t => t.id === tipo.id);
    if (indice === -1) {
      throw new Error('Room type not found: ' + tipo.id);
    }
    const nombre = this.validarNombre(tipo.nombre, tipo.id);
    const actualizado: TipoHabitacion = { ...tipo, nombre };
    this.tiposHabitacion[indice] = actualizado;
    return actualizado;
  }

  eliminar(id: number): void {
    const indice = this.tiposHabitacion.findIndex(t => t.id === id);
    if (indice === -1) {
      throw new Error('Room type not found: ' + id);
    }
    if (this.habitacionService.existePorTipoHabitacionId(id)) {
      throw new Error('This room type still has associated rooms and cannot be deleted.');
    }
    this.tiposHabitacion.splice(indice, 1);
  }

  // Nombre obligatorio y único (ignorando mayúsculas); idActual excluye al propio tipo al editar
  private validarNombre(nombre: string, idActual: number | null): string {
    const limpio = (nombre ?? '').trim();
    if (limpio === '') {
      throw new Error('Room type name is required.');
    }
    const conMismoNombre = this.tiposHabitacion.find(t => t.nombre.toLowerCase() === limpio.toLowerCase());
    if (conMismoNombre && conMismoNombre.id !== idActual) {
      throw new Error('A room type with name ' + limpio + ' already exists.');
    }
    return limpio;
  }
}