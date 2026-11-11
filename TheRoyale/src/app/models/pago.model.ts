import { Cuenta } from './cuenta.model';
import { Operador } from './operador.model';

// Entidad Pago completa (con sus relaciones anidadas) del backend.
// Las pantallas usan la versión plana de cuenta.model.ts, que es la que devuelve el API de cuentas.
export interface Pago {
  id: number;
  cuenta: Cuenta;
  operador: Operador | null;
  monto: number;
  fecha: string;
  metodoPago: string;
}
