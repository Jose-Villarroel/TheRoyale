import { Cuenta } from './cuenta.model';
import { Operador } from './operador.model';
import { Servicio } from './servicio.model';

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
