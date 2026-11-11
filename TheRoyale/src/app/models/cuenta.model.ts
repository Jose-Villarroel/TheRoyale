export type EstadoCuenta = 'ABIERTA' | 'CERRADA';

// Refleja CuentaResponse del backend: la entidad Cuenta no serializa sus listas,
// así que el API devuelve este DTO ya con los totales calculados.
export interface ItemConsumo {
  id: number;
  servicioId: number;
  servicioNombre: string;
  operadorNombre: string;
  cantidad: number;
  fechaHora: string;
  precioUnitario: number;
  total: number;
  pagado: boolean;
}

export interface Pago {
  id: number;
  monto: number;
  fecha: string;
  metodoPago: string;
  operadorNombre: string | null;
}

export interface Cuenta {
  id: number;
  reservaId: number;
  estado: EstadoCuenta;
  fechaCreacion: string;
  items: ItemConsumo[];
  pagos: Pago[];
  totalConsumos: number;
  totalPagado: number;
  saldo: number;
}

export interface AgregarConsumoRequest {
  servicioId: number;
  cantidad: number;
  operadorId: number | null;
}

export interface RegistrarPagoRequest {
  monto: number;
  metodoPago: string;
  operadorId: number | null;
}
