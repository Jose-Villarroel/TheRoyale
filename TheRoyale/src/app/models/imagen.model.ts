import { TipoHabitacion } from './tipo-habitacion.model';

export interface Imagen {
  id: number;
  tipoHabitacion: TipoHabitacion;
  url: string;
  orden: number;
}
