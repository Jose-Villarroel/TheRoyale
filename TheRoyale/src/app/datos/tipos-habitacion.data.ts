import { TipoHabitacion } from '../models/tipo-habitacion.model';

// ===== "Base de datos" quemada de tipos de habitación, calcada del DataLoader del backend =====
// Vive aquí (y no dentro de un servicio) para que HabitacionService y TipoHabitacionService
// compartan las MISMAS instancias sin inyectarse entre ellos (eso crearía un ciclo de dependencias).
export const TIPOS_HABITACION: TipoHabitacion[] = [
  { id: 1, nombre: 'Normal', descripcion: 'Elegant essentials for a refined stay. 1-2 guests, double bed, private bathroom, premium Wi-Fi.', imagenUrl: '/images/suite-3.webp' },
  { id: 2, nombre: 'Executive', descripcion: 'Designed for those who work while they travel. 1-2 guests, executive workspace, king bed, premium Wi-Fi.', imagenUrl: '/images/suite-1.webp' },
  { id: 3, nombre: 'VIP', descripcion: 'A private space to relax and unwind. Up to 3 guests, separate living area, premium amenities, city views.', imagenUrl: '/images/suite-2.webp' },
  { id: 4, nombre: 'Luxury', descripcion: 'The ultimate expression of The Royale. Up to 4 guests, full suite, jacuzzi, privileged city view.', imagenUrl: '/images/suite-4.webp' },
  { id: 5, nombre: 'Presidential Suite', descripcion: 'Unmatched exclusivity. Up to 6 guests, private terrace, butler service, panoramic city view, private dining.', imagenUrl: '/images/luxury 1.jpg' }
];

// Busca un tipo por su id dentro de la semilla. Lanza error si no existe para que un id mal
// escrito en los datos quemados se note al arrancar y no deje un undefined dando vueltas.
export function tipoHabitacionSemilla(id: number): TipoHabitacion {
  const tipo = TIPOS_HABITACION.find(t => t.id === id);
  if (!tipo) {
    throw new Error('Tipo de habitación inexistente en los datos semilla: ' + id);
  }
  return tipo;
}
