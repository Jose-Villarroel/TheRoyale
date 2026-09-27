export interface Pago {
  id: number;
  cuentaId: number;
  operadorId: number | null;
  monto: number;
  fecha: string;
  metodoPago: string;
}