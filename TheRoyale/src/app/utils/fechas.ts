import { Reserva } from '../models/reserva.model';

const MS_POR_DIA = 86400000;

// Fecha local en formato yyyy-MM-dd. toISOString() da la fecha en UTC: en Colombia,
// después de las 7 p.m. ya marcaría el día siguiente.
export function aTexto(fecha: Date): string {
  const anio = fecha.getFullYear();
  const mes = String(fecha.getMonth() + 1).padStart(2, '0');
  const dia = String(fecha.getDate()).padStart(2, '0');
  return `${anio}-${mes}-${dia}`;
}

export function hoyLocal(): string {
  return aTexto(new Date());
}

// Día siguiente a una fecha yyyy-MM-dd: la salida nunca puede caer el mismo día que la llegada
export function diaSiguiente(fecha: string): string {
  const siguiente = new Date(`${fecha}T00:00:00`);
  siguiente.setDate(siguiente.getDate() + 1);
  return aTexto(siguiente);
}

// Noches entre dos fechas yyyy-MM-dd; se leen como medianoche local
export function noches(inicio: string, fin: string): number {
  const ms = new Date(`${fin}T00:00:00`).getTime() - new Date(`${inicio}T00:00:00`).getTime();
  return Math.round(ms / MS_POR_DIA);
}

// El backend solo deja cancelar una reserva vigente cuya llegada sea posterior a hoy (AC22)
export function puedeCancelar(reserva: Pick<Reserva, 'estado' | 'fechaInicio'>): boolean {
  const vigente = reserva.estado === 'PENDIENTE' || reserva.estado === 'CONFIRMADA';
  return vigente && reserva.fechaInicio > hoyLocal();
}
