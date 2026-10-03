import { Injectable } from '@angular/core';
import { Servicio } from '../models/servicio.model';
import { SERVICIOS } from '../datos/servicio.data';

@Injectable({ providedIn: 'root' })
export class ServicioService {

  // ===== "Base de datos" quemada, vive en memoria (F5 vuelve a estos datos) =====
  private servicios: Servicio[] = SERVICIOS;

  obtenerTodos(): Servicio[] {
    return this.servicios;
  }

  obtenerPorId(id: number): Servicio | undefined {
    return this.servicios.find(servicio => servicio.id === id);
  }

  // ===== CRUD =====
  crear(servicio: Omit<Servicio, 'id'>): Servicio {
    const nombre = this.validarNombre(servicio.nombre, null);
    const precio = this.validarPrecio(servicio.precio);
    const siguienteId = Math.max(0, ...this.servicios.map(s => s.id)) + 1;
    const nuevo: Servicio = { ...servicio, id: siguienteId, nombre, precio };
    this.servicios.push(nuevo);
    return nuevo;
  }

  actualizar(servicio: Servicio): Servicio {
    const indice = this.servicios.findIndex(s => s.id === servicio.id);
    if (indice === -1) {
      throw new Error('Service not found: ' + servicio.id);
    }
    const nombre = this.validarNombre(servicio.nombre, servicio.id);
    const precio = this.validarPrecio(servicio.precio);

    // Se modifica el objeto existente en vez de reemplazarlo, igual que en TipoHabitacionService,
    // por si en el futuro algo más (ej. un ItemConsumo) guarda una referencia directa a este objeto.
    const actual = this.servicios[indice];
    Object.assign(actual, servicio, { nombre, precio });
    return actual;
  }

  eliminar(id: number): void {
    const indice = this.servicios.findIndex(s => s.id === id);
    if (indice === -1) {
      throw new Error('Service not found: ' + id);
    }
    this.servicios.splice(indice, 1);
  }

  // Nombre obligatorio y único (ignorando mayúsculas); idActual excluye al propio servicio al editar
  private validarNombre(nombre: string, idActual: number | null): string {
    const limpio = (nombre ?? '').trim();
    if (limpio === '') {
      throw new Error('Service name is required.');
    }
    const conMismoNombre = this.servicios.find(s => s.nombre.toLowerCase() === limpio.toLowerCase());
    if (conMismoNombre && conMismoNombre.id !== idActual) {
      throw new Error('A service with name ' + limpio + ' already exists.');
    }
    return limpio;
  }

  private validarPrecio(precio: number): number {
    if (precio === null || precio === undefined || isNaN(precio) || precio <= 0) {
      throw new Error('Price must be a number greater than 0.');
    }
    return precio;
  }
}