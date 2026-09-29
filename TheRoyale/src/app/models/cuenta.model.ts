import { Reserva } from './reserva.model';

export type EstadoCuenta = 'ABIERTA' | 'CERRADA';

export interface Cuenta {
  id: number;
  reserva: Reserva;
  estado: EstadoCuenta;
  fechaCreacion: string;
}
