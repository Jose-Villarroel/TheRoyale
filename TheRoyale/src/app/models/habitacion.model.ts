export type EstadoHabitacion = 'DISPONIBLE' | 'OCUPADA' | 'MANTENIMIENTO' | 'DESHABILITADA';

// El backend devuelve la habitación plana: del tipo solo viajan su id y su nombre
export interface Habitacion {
  id: number;
  numero: string;
  precio: number;
  estado: EstadoHabitacion;
  tipoHabitacionId: number;
  tipoHabitacionNombre: string;
}

// Cuerpo de POST/PUT: el tipo se referencia por id
export interface HabitacionRequest {
  numero: string;
  precio: number;
  estado: EstadoHabitacion;
  tipoHabitacionId: number;
}
