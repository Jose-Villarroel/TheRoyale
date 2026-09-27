export type EstadoHabitacion = 'DISPONIBLE' | 'OCUPADA' | 'MANTENIMIENTO' | 'DESHABILITADA';

export interface Habitacion {
  id: number;
  numero: string;
  tipoHabitacionId: number;
  precio: number;
  estado: EstadoHabitacion;
}