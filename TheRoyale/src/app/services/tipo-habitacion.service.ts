import { Injectable } from '@angular/core';
import { TipoHabitacion } from '../models/tipo-habitacion.model';

@Injectable({ providedIn: 'root' })
export class TipoHabitacionService {

  // ===== "Base de datos" quemada, con los mismos 5 tipos que ya tienes en el backend =====
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
}