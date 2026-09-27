export type EstadoCuenta = 'ABIERTA' | 'CERRADA';

export interface Cuenta {
  id: number;
  reservaId: number;
  estado: EstadoCuenta;
  fechaCreacion: string;
}