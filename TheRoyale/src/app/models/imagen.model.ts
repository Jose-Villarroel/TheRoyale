import { TipoHabitacion } from './tipo-habitacion.model';

// Imagen adicional de un tipo de habitación (galería); "orden" define su posición.
export interface Imagen {
  id: number;
  tipoHabitacion: TipoHabitacion;
  url: string;
  orden: number;
}
