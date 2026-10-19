import { Cliente } from './cliente.model';
import { Habitacion } from './habitacion.model';

export type EstadoReserva = 'PENDIENTE' | 'CONFIRMADA' | 'EN_CURSO' | 'CANCELADA' | 'FINALIZADA';

export interface Reserva {
  id: number;
  cliente: Cliente;
  habitacion: Habitacion;
  fechaInicio: string;
  fechaFin: string;
  estado: EstadoReserva;
  cantidadPersonas: number;
  precioNocheAcordado: number;
  fechaCreacion: string;
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
