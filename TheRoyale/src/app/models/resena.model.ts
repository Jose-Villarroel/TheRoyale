export interface Resena {
  id: number;
  clienteId: number;
  clienteNombre: string;
  clienteApellido: string;
  tipoHabitacionId: number;
  tipoHabitacionNombre: string;
  estrellas: number;
  titulo: string;
  comentario: string;
  fecha: string;
}

export interface CrearResenaRequest {
  clienteId: number;
  tipoHabitacionId: number;
  estrellas: number;
  titulo: string;
  comentario: string;
}

export interface ResumenResenas {
  promedioEstrellas: number;
  totalResenas: number;
  conteoPorEstrellas: Record<number, number>;
  resenas: Resena[];
}
