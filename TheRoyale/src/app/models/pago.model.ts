import { Cuenta } from './cuenta.model';
import { Operador } from './operador.model';

export interface Pago {
  id: number;
  cuenta: Cuenta;
  operador: Operador | null;
  monto: number;
  fecha: string;
  metodoPago: string;
}
