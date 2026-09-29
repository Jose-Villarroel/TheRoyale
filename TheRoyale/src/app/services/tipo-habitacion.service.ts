import { Injectable } from '@angular/core';
import { TipoHabitacion } from '../models/tipo-habitacion.model';
import { TIPOS_HABITACION } from '../datos/tipos-habitacion.data';
import { HabitacionService } from './habitacion.service';

@Injectable({ providedIn: 'root' })
export class TipoHabitacionService {

  constructor(private habitacionService: HabitacionService) {}

  // ===== "Base de datos" quemada, con los mismos 5 tipos que ya tienes en el backend =====
  // Vive en memoria: los cambios se ven mientras no se recargue la página (F5 vuelve a estos datos).
  // Es el MISMO array de objetos que usan las habitaciones (datos/tipos-habitacion.data.ts),
  // así que las habitaciones siempre ven el tipo actualizado.
  private tiposHabitacion: TipoHabitacion[] = TIPOS_HABITACION;

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

    // Se modifica el objeto existente en vez de reemplazarlo: las habitaciones guardan una
    // referencia a este mismo objeto, y crear uno nuevo las dejaría con los datos viejos.
    const actual = this.tiposHabitacion[indice];
    Object.assign(actual, tipo, { nombre });
    return actual;
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
