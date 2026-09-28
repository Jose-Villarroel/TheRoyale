export type EstadoReserva = 'PENDIENTE' | 'CONFIRMADA' | 'EN_CURSO' | 'CANCELADA' | 'FINALIZADA';

export interface Reserva {
  id: number;
  clienteId: number;
  habitacionId: number;
  fechaInicio: string;
  fechaFin: string;
  estado: EstadoReserva;
  cantidadPersonas: number;
  precioNocheAcordado: number;
  fechaCreacion: string;
}
