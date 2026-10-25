export interface Operador {
  id: number;
  nombre: string;
  email: string;
  // Vacío al editar significa "conservar la actual": el backend la ignora si no llega
  password?: string;
  tipo: 'OPERADOR';
}
