export interface ItemConsumo {
  id: number;
  cuentaId: number;
  servicioId: number;
  operadorId: number;
  cantidad: number;
  fechaHora: string;
  precioUnitario: number;
  pagado: boolean;
}