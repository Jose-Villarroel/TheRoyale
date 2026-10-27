import { Cuenta } from './cuenta.model';
import { Operador } from './operador.model';
import { Servicio } from './servicio.model';

// Entidad ItemConsumo completa (con sus relaciones anidadas) del backend.
// Las pantallas usan la versión plana de cuenta.model.ts, que es la que devuelve el API de cuentas.
export interface ItemConsumo {
  id: number;
  cuenta: Cuenta;
  servicio: Servicio;
  operador: Operador;
  cantidad: number;
  fechaHora: string;
  precioUnitario: number;
  pagado: boolean;
}
