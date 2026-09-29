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
