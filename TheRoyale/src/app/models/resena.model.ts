// Modelos de las reseñas de habitaciones tal como los expone el API REST (/api/resenas).

// ResenaResponse del backend: la reseña aplanada con los datos de la estadía y del tipo.
// clienteNombre ya viene recortado a "Nombre I." (el público nunca ve email, cédula ni teléfono).
export interface Resena {
  id: number;
  calificacion: number;
  titulo: string | null;
  comentario: string;
  visible: boolean;
  fechaCreacion: string;
  fechaModificacion: string | null;
  reservaId: number;
  fechaInicioEstadia: string;
  fechaFinEstadia: string;
  clienteId: number;
  clienteNombre: string;
  tipoHabitacionId: number;
  tipoHabitacionNombre: string;
}

// ResumenResenasResponse: promedio null y total 0 cuando el tipo no tiene reseñas visibles.
// distribucion trae siempre las claves 1..5 (en el JSON llegan como texto: "1".."5").
export interface ResumenResenas {
  tipoHabitacionId: number;
  promedio: number | null;
  total: number;
  distribucion: Record<number, number>;
}

// CrearResenaRequest: mientras no haya JWT el clienteId viaja en el cuerpo
export interface CrearResena {
  reservaId: number;
  clienteId: number;
  calificacion: number;
  titulo: string | null;
  comentario: string;
}

// El PUT usa el mismo request sin reservaId: la reseña no cambia de reserva ni de tipo (RN-7)
export type ActualizarResena = Omit<CrearResena, 'reservaId'>;

// Campos que llena el huésped en el formulario; la página agrega reservaId y clienteId
export type DatosResena = Pick<CrearResena, 'calificacion' | 'titulo' | 'comentario'>;

// Filtros opcionales de la vista de moderación del admin
export interface FiltrosModeracion {
  tipoHabitacionId?: number | null;
  calificacion?: number | null;
}

// Pestañas de visibilidad del listado admin; null = "All"
export type EstadoVisibilidad = 'VISIBLE' | 'OCULTA';

export const FILTROS_VISIBILIDAD_RESENA: readonly { etiqueta: string; estado: EstadoVisibilidad | null }[] = [
  { etiqueta: 'All', estado: null },
  { etiqueta: 'Visible', estado: 'VISIBLE' },
  { etiqueta: 'Hidden', estado: 'OCULTA' }
];

// Límites de RN-4 y RN-5, compartidos por el formulario y sus mensajes
export const RESENA_COMENTARIO_MIN = 10;
export const RESENA_COMENTARIO_MAX = 1000;
export const RESENA_TITULO_MAX = 100;
