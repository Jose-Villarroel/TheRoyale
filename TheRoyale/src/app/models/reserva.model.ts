export type EstadoReserva = 'PENDIENTE' | 'CONFIRMADA' | 'CANCELADA' | 'FINALIZADA';

export interface Reserva {
  id: number;
  clienteId: number;
  habitacionId: number;
  operadorId: number | null;
  fechaInicio: string;
  fechaFin: string;
  estado: EstadoReserva;
  cantidadPersonas: number;
  precioNocheAcordado: number;
  fechaCreacion: string;
}