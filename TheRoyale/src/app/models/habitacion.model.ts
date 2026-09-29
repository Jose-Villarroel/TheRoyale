import { TipoHabitacion } from './tipo-habitacion.model';

export type EstadoHabitacion = 'DISPONIBLE' | 'OCUPADA' | 'MANTENIMIENTO' | 'DESHABILITADA';

export interface Habitacion {
  id: number;
  numero: string;
  tipoHabitacion: TipoHabitacion;
  precio: number;
  estado: EstadoHabitacion;
}
