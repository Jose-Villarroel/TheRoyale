export interface Cliente {
  id: number;
  nombre: string;
  apellido: string;
  email: string;
  cedula: string;
  password?: string;
  telefono?: string;
  fechaRegistro?: string;
}