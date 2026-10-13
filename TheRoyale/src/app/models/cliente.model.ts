export interface Cliente {
  id: number;
  nombre: string;
  apellido: string;
  email: string;
  password?: string;
  telefono?: string;
  fechaRegistro?: string;
}