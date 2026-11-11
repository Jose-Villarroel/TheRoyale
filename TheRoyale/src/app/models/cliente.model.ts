// Huésped registrado. Es el usuario que inicia sesión en /login y hace reservas.
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