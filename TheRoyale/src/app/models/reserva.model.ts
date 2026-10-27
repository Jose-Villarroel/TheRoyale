// Modelos de la reserva tal como los expone el API REST de reservas.
export type EstadoReserva = 'PENDIENTE' | 'CONFIRMADA' | 'EN_CURSO' | 'CANCELADA' | 'FINALIZADA';

// Pestaña del filtro por estado; estado null = "All" (sin filtro)
export interface FiltroEstadoReserva {
  etiqueta: string;
  estado: EstadoReserva | null;
}

// Filtros compartidos por el listado admin y el del operador (antes duplicados en ambos).
// Los valores son los del enum EstadoReserva del backend.
export const FILTROS_ESTADO_RESERVA: readonly FiltroEstadoReserva[] = [
  { etiqueta: 'All', estado: null },
  { etiqueta: 'Pending', estado: 'PENDIENTE' },
  { etiqueta: 'Confirmed', estado: 'CONFIRMADA' },
  { etiqueta: 'In Progress', estado: 'EN_CURSO' },
  { etiqueta: 'Cancelled', estado: 'CANCELADA' },
  { etiqueta: 'Completed', estado: 'FINALIZADA' }
];

// El backend devuelve la reserva plana: datos del cliente, la habitación y su tipo ya aplanados
export interface Reserva {
  id: number;
  fechaInicio: string;
  fechaFin: string;
  estado: EstadoReserva;
  cantidadPersonas: number;
  precioNocheAcordado: number;
  fechaCreacion: string;
  clienteId: number;
  clienteNombre: string;
  clienteApellido: string;
  clienteEmail: string;
  clienteTelefono: string | null;
  clienteCedula: string | null;
  habitacionId: number;
  habitacionNumero: string;
  tipoHabitacionId: number;
  tipoHabitacionNombre: string;
  tipoHabitacionImagenUrl: string | null;
}

// Lo que espera CrearReservaRequest del backend: el cliente pide un TIPO, no una habitación
export interface CrearReservaRequest {
  clienteId: number;
  tipoHabitacionId: number;
  fechaInicio: string;
  fechaFin: string;
  cantidadPersonas: number;
}

// precioDesde es null cuando no queda ninguna habitación libre de ese tipo
export interface DisponibilidadTipo {
  tipoHabitacionId: number;
  nombre: string;
  descripcion: string;
  imagenUrl: string;
  habitacionesDisponibles: number;
  precioDesde: number | null;
}
